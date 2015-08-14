FROM elasticsearch:latest

RUN plugin -i mobz/elasticsearch-head
RUN plugin -i elasticsearch/marvel/latest

EXPOSE 9200 9300

CMD ["elasticsearch"]
