param(
    [string]$EsUrl = "http://127.0.0.1:19200",
    [string]$MysqlContainer = "mysql"
)

$ErrorActionPreference = "Stop"

$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$settingsPath = Join-Path $root "services\search-service\src\main\resources\elasticsearch\product-settings.json"
$mappingPath = Join-Path $root "services\search-service\src\main\resources\elasticsearch\product-mapping.json"

function Invoke-MySqlQuery {
    param([string]$Sql)

    $output = & docker exec $MysqlContainer mysql -uroot -proot --batch --raw --skip-column-names -e $Sql
    if ($LASTEXITCODE -ne 0) {
        throw "MySQL query failed"
    }
    return $output
}

function Convert-NullableString {
    param([object]$Value)

    if ($null -eq $Value) {
        return $null
    }
    $text = [string]$Value
    if ([string]::IsNullOrWhiteSpace($text)) {
        return $null
    }
    return $text
}

function Convert-ToInt {
    param([object]$Value)

    if ($null -eq $Value -or [string]::IsNullOrWhiteSpace([string]$Value)) {
        return 0
    }
    return [int]$Value
}

function Convert-ToDouble {
    param([object]$Value)

    if ($null -eq $Value -or [string]::IsNullOrWhiteSpace([string]$Value)) {
        return $null
    }
    return [double]$Value
}

$header = @(
    "productId",
    "shopId",
    "productName",
    "price",
    "stockQuantity",
    "categoryId",
    "categoryName",
    "brandId",
    "status",
    "description",
    "imageUrl",
    "detailImages",
    "sku",
    "salesCount",
    "createdAt",
    "updatedAt"
) -join "`t"

$sql = @"
USE product_db;
SELECT
  sp.id AS productId,
  sp.merchant_id AS shopId,
  sp.spu_name AS productName,
  (
    SELECT s.sale_price
    FROM sku s
    WHERE s.spu_id = sp.id AND IFNULL(s.deleted, 0) = 0 AND s.status = 1
    ORDER BY s.sale_price ASC, s.id ASC
    LIMIT 1
  ) AS price,
  COALESCE(st.total_available, 0) AS stockQuantity,
  sp.category_id AS categoryId,
  c.name AS categoryName,
  sp.brand_id AS brandId,
  sp.status AS status,
  sp.description AS description,
  COALESCE(
    (
      SELECT s.image_url
      FROM sku s
      WHERE s.spu_id = sp.id AND IFNULL(s.deleted, 0) = 0 AND s.status = 1
      ORDER BY s.sale_price ASC, s.id ASC
      LIMIT 1
    ),
    sp.main_image
  ) AS imageUrl,
  (
    SELECT GROUP_CONCAT(DISTINCT img.image_url ORDER BY img.id SEPARATOR ',')
    FROM sku img
    WHERE img.spu_id = sp.id
      AND IFNULL(img.deleted, 0) = 0
      AND img.image_url IS NOT NULL
      AND img.image_url <> ''
  ) AS detailImages,
  (
    SELECT s.sku_code
    FROM sku s
    WHERE s.spu_id = sp.id AND IFNULL(s.deleted, 0) = 0 AND s.status = 1
    ORDER BY s.sale_price ASC, s.id ASC
    LIMIT 1
  ) AS sku,
  COALESCE(oi.sales_count, 0) AS salesCount,
  sp.created_at AS createdAt,
  sp.updated_at AS updatedAt
FROM spu sp
LEFT JOIN category c ON c.id = sp.category_id AND IFNULL(c.deleted, 0) = 0
LEFT JOIN (
  SELECT k.spu_id, SUM(ss.available_qty) AS total_available
  FROM sku k
  LEFT JOIN stock_db.stock_segment ss
    ON ss.sku_id = k.id
   AND IFNULL(ss.deleted, 0) = 0
   AND ss.status = 1
  WHERE IFNULL(k.deleted, 0) = 0
  GROUP BY k.spu_id
) st ON st.spu_id = sp.id
LEFT JOIN (
  SELECT spu_id, SUM(quantity) AS sales_count
  FROM order_db.order_item
  WHERE IFNULL(deleted, 0) = 0
  GROUP BY spu_id
) oi ON oi.spu_id = sp.id
WHERE IFNULL(sp.deleted, 0) = 0
  AND sp.status = 1
ORDER BY sp.id ASC;
"@

$rows = Invoke-MySqlQuery -Sql $sql
$table = @($header) + @($rows)
$products = $table | ConvertFrom-Csv -Delimiter "`t"

$settings = Get-Content $settingsPath -Raw | ConvertFrom-Json
$mappings = Get-Content $mappingPath -Raw | ConvertFrom-Json
$indexBody = @{
    settings = $settings
    mappings = $mappings
} | ConvertTo-Json -Depth 20

try {
    Invoke-WebRequest -UseBasicParsing -Method Delete -Uri "$EsUrl/product_index" | Out-Null
} catch {
}

Invoke-WebRequest -UseBasicParsing -Method Put -Uri "$EsUrl/product_index" -ContentType "application/json" -Body $indexBody | Out-Null

$now = Get-Date
$bulkLines = New-Object System.Collections.Generic.List[string]

foreach ($product in $products) {
    $createdAt = [datetime]$product.createdAt
    $updatedAt = [datetime]$product.updatedAt
    $salesCount = Convert-ToInt $product.salesCount
    $isNew = $createdAt -gt $now.AddDays(-30)
    $isHot = $salesCount -gt 0
    $recommended = $true
    $hotScore = 30.0
    if ($isNew) { $hotScore += 15.0 }
    if ($isHot) { $hotScore += [math]::Log(1 + $salesCount) * 25.0 }

    $doc = [ordered]@{
        id = [string]$product.productId
        productId = [int64]$product.productId
        shopId = [int64]$product.shopId
        shopName = $null
        productName = Convert-NullableString $product.productName
        productNameKeyword = Convert-NullableString $product.productName
        price = Convert-ToDouble $product.price
        stockQuantity = Convert-ToInt $product.stockQuantity
        categoryId = [int64]$product.categoryId
        categoryName = Convert-NullableString $product.categoryName
        categoryNameKeyword = Convert-NullableString $product.categoryName
        brandId = if ([string]::IsNullOrWhiteSpace([string]$product.brandId)) { $null } else { [int64]$product.brandId }
        brandName = $null
        brandNameKeyword = $null
        merchantId = [int64]$product.shopId
        merchantName = $null
        status = Convert-ToInt $product.status
        description = Convert-NullableString $product.description
        imageUrl = Convert-NullableString $product.imageUrl
        detailImages = Convert-NullableString $product.detailImages
        tags = @()
        sku = Convert-NullableString $product.sku
        salesCount = $salesCount
        rating = $null
        reviewCount = 0
        recommended = $recommended
        isNew = $isNew
        isHot = $isHot
        createdAt = $createdAt.ToString("yyyy-MM-dd HH:mm:ss")
        updatedAt = $updatedAt.ToString("yyyy-MM-dd HH:mm:ss")
        hotScore = $hotScore
        searchWeight = $hotScore + 10.0
        remark = "local-rebuild"
    }

    $bulkLines.Add((@{ index = @{ _index = "product_index"; _id = [string]$product.productId } } | ConvertTo-Json -Compress))
    $bulkLines.Add(($doc | ConvertTo-Json -Compress -Depth 10))
}

$bulkBody = ($bulkLines -join "`n") + "`n"
$bulkFile = Join-Path $env:TEMP "cloud-product-index-bulk.ndjson"
[System.IO.File]::WriteAllText($bulkFile, $bulkBody, [System.Text.UTF8Encoding]::new($false))

$bulkResponse = Invoke-WebRequest -UseBasicParsing -Method Post -Uri "$EsUrl/_bulk?refresh=true" -ContentType "application/x-ndjson" -InFile $bulkFile
$bulkResult = $bulkResponse.Content | ConvertFrom-Json
if ($bulkResult.errors) {
    throw "Elasticsearch bulk import reported errors"
}

Write-Host ("SEARCH_INDEX_REBUILT docs={0}" -f $products.Count)
