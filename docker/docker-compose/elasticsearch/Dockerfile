FROM docker.elastic.co/elasticsearch/elasticsearch:8.18.1

RUN /usr/share/elasticsearch/bin/elasticsearch-plugin install --batch \
    https://release.infinilabs.com/analysis-pinyin/stable/elasticsearch-analysis-pinyin-8.18.1.zip
