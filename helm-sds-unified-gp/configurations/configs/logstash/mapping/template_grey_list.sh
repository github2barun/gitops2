curl -u elastic:seamless -XPUT "http://localhost:9200/_template/grey_list?pretty" -H 'Content-Type: application/json' -d'{
  "index_patterns": [
    "grey_list"
  ],
  "settings": {
    "index": {
      "codec": "best_compression",
      "refresh_interval": "30s",
      "number_of_shards": "2",
      "auto_expand_replicas": "0-1"
    }
  },
  "mappings": {
    "properties": {
      "subscriberMSISDN": {
        "type": "keyword"
      },
      "grey_status":{
       "type": "keyword"
      },
      "timestamp": {
        "type": "date"
      }
    }
  }
}'