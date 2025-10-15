package com.cloud.common.converter;

/**
 * MapStruct转换器使用指南
 *
 * <h2>快速开始</h2>
 * <pre>{@code
 * @Mapper(
 *     componentModel = "spring",
 *     unmappedTargetPolicy = ReportingPolicy.IGNORE,
 *     unmappedSourcePolicy = ReportingPolicy.IGNORE
 * )
 * public interface UserConverter extends BaseConverter<User, UserDTO, UserVO> {
 *     // MapStruct会自动实现BaseConverter中的所有方法
 *     // 只需要定义特殊的转换方法
 *
 *     @Mapping(target = "id", ignore = true)
 *     @Mapping(target = "createdAt", ignore = true)
 *     User toEntity(RegisterDTO registerDTO);
 * }
 * }</pre>
 *
 * <h2>注解说明</h2>
 * <ul>
 *     <li>@Mapper - 标记为MapStruct转换器接口</li>
 *     <li>componentModel = "spring" - 生成Spring Bean</li>
 *     <li>unmappedTargetPolicy = IGNORE - 忽略未映射的目标字段</li>
 *     <li>unmappedSourcePolicy = IGNORE - 忽略未映射的源字段</li>
 * </ul>
 *
 * <h2>常用映射注解</h2>
 * <ul>
 *     <li>@Mapping(source = "fieldA", target = "fieldB") - 字段名映射</li>
 *     <li>@Mapping(target = "field", ignore = true) - 忽略字段</li>
 *     <li>@Mapping(target = "field", constant = "value") - 常量值</li>
 *     <li>@Mapping(target = "field", expression = "java(...)") - 自定义表达式</li>
 *     <li>@Mapping(target = "field", qualifiedByName = "methodName") - 使用自定义方法</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 *
 * <h3>1. 基础转换（继承BaseConverter）</h3>
 * <pre>{@code
 * @Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
 * public interface ProductConverter extends BaseConverter<Product, ProductDTO, ProductVO> {
 *     // 自动获得toDTO、toEntity、toVO等所有方法
 * }
 * }</pre>
 *
 * <h3>2. 简单转换（继承SimpleConverter）</h3>
 * <pre>{@code
 * @Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
 * public interface CategoryConverter extends SimpleConverter<Category, CategoryDTO> {
 *     // 自动获得toDTO、toEntity、toDTOList、toEntityList方法
 * }
 * }</pre>
 *
 * <h3>3. 字段名不同的映射</h3>
 * <pre>{@code
 * @Mapper(componentModel = "spring")
 * public interface OrderConverter {
 *     @Mapping(source = "userName", target = "customerName")
 *     @Mapping(source = "createTime", target = "orderTime")
 *     OrderDTO toDTO(Order order);
 * }
 * }</pre>
 *
 * <h3>4. 忽略特定字段</h3>
 * <pre>{@code
 * @Mapper(componentModel = "spring")
 * public interface UserConverter {
 *     @Mapping(target = "id", ignore = true)
 *     @Mapping(target = "password", ignore = true)  // 安全：不转换密码
 *     @Mapping(target = "createdAt", ignore = true)
 *     @Mapping(target = "updatedAt", ignore = true)
 *     User toEntity(UserDTO dto);
 * }
 * }</pre>
 *
 * <h3>5. 使用常量值</h3>
 * <pre>{@code
 * @Mapper(componentModel = "spring")
 * public interface UserConverter {
 *     @Mapping(target = "status", constant = "ACTIVE")
 *     @Mapping(target = "userType", constant = "NORMAL")
 *     User toEntity(RegisterDTO dto);
 * }
 * }</pre>
 *
 * <h3>6. 自定义转换逻辑</h3>
 * <pre>{@code
 * @Mapper(componentModel = "spring")
 * public interface OrderConverter {
 *     @Mapping(target = "statusText", qualifiedByName = "statusToText")
 *     OrderVO toVO(Order order);
 *
 *     @Named("statusToText")
 *     default String statusToText(Integer status) {
 *         return switch (status) {
 *             case 0 -> "待支付";
 *             case 1 -> "已支付";
 *             case 2 -> "已取消";
 *             default -> "未知";
 *         };
 *     }
 * }
 * }</pre>
 *
 * <h3>7. 使用表达式</h3>
 * <pre>{@code
 * @Mapper(componentModel = "spring")
 * public interface UserConverter {
 *     @Mapping(target = "fullName",
 *              expression = "java(user.getFirstName() + \" \" + user.getLastName())")
 *     @Mapping(target = "age",
 *              expression = "java(java.time.Period.between(user.getBirthday(), java.time.LocalDate.now()).getYears())")
 *     UserDTO toDTO(User user);
 * }
 * }</pre>
 *
 * <h3>8. 复用其他Mapper</h3>
 * <pre>{@code
 * @Mapper(componentModel = "spring", uses = {AddressConverter.class})
 * public interface UserConverter {
 *     UserDTO toDTO(User user);  // 会自动使用AddressConverter转换address字段
 * }
 * }</pre>
 *
 * <h3>9. 日期时间格式化</h3>
 * <pre>{@code
 * @Mapper(componentModel = "spring")
 * public interface OrderConverter {
 *     @Mapping(target = "createTime", dateFormat = "yyyy-MM-dd HH:mm:ss")
 *     OrderDTO toDTO(Order order);
 * }
 * }</pre>
 *
 * <h3>10. 嵌套对象映射</h3>
 * <pre>{@code
 * @Mapper(componentModel = "spring")
 * public interface OrderConverter {
 *     @Mapping(source = "user.name", target = "userName")
 *     @Mapping(source = "user.phone", target = "userPhone")
 *     @Mapping(source = "address.province", target = "province")
 *     OrderDTO toDTO(Order order);
 * }
 * }</pre>
 *
 * <h2>最佳实践</h2>
 * <ol>
 *     <li><b>统一配置</b>：所有Mapper使用相同的componentModel和ReportingPolicy</li>
 *     <li><b>继承基础接口</b>：使用BaseConverter或SimpleConverter统一方法签名</li>
 *     <li><b>忽略敏感字段</b>：密码等敏感信息不要在toDTO中转换</li>
 *     <li><b>忽略自动填充字段</b>：createdAt、updatedAt等由框架自动填充的字段应该忽略</li>
 *     <li><b>复杂逻辑放Service</b>：Converter只做简单映射，复杂业务逻辑放在Service层</li>
 *     <li><b>使用default方法</b>：需要额外处理的字段，使用default方法配合@Named注解</li>
 *     <li><b>列表转换</b>：MapStruct会自动生成List转换方法</li>
 *     <li><b>null安全</b>：MapStruct自动处理null值，无需手动检查</li>
 * </ol>
 *
 * <h2>常见问题</h2>
 * <ul>
 *     <li><b>Q: 编译时提示"unmapped target property"？</b><br/>
 *         A: 使用unmappedTargetPolicy = ReportingPolicy.IGNORE忽略，或用@Mapping显式映射</li>
 *
 *     <li><b>Q: 如何映射不同类型的字段（如Long到String）？</b><br/>
 *         A: MapStruct会自动转换，或使用expression自定义转换</li>
 *
 *     <li><b>Q: 如何在转换后执行额外操作？</b><br/>
 *         A: 使用@AfterMapping注解标记的default方法</li>
 *
 *     <li><b>Q: 生成的实现类在哪里？</b><br/>
 *         A: target/generated-sources/annotations目录下</li>
 *
 *     <li><b>Q: 如何调试MapStruct生成的代码？</b><br/>
 *         A: 查看target/generated-sources/annotations下的实现类</li>
 * </ul>
 *
 * <h2>性能说明</h2>
 * <ul>
 *     <li>MapStruct在编译期生成代码，运行时<b>零性能开销</b></li>
 *     <li>生成的代码是直接的字段赋值，性能等同手写代码</li>
 *     <li>比反射（如BeanUtils）快100倍以上</li>
 *     <li>完全类型安全，编译期检查</li>
 * </ul>
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
public final class MapStructGuide {
    private MapStructGuide() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
