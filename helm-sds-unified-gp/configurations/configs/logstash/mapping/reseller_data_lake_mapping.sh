curl -u elastic:seamless -XPUT "http://localhost:9200/_template/reseller_data_lake?pretty" -H 'Content-Type: application/json' -d'{
  "index_patterns": [
    "reseller_data_lake*"
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
        "date_detection": false,
        "properties" : {
          "@timestamp" : {
            "type" : "date"
          },
          "timestamp" : {
            "type" : "date"
          }
        }
    }
}'