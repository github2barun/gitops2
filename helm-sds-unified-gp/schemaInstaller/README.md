# helm-db-install

This chart can be used to install/upgrade database schemas, and install locale
data for ERS 5.0
Utility depends on indivisual database schemas be present for all the components
with below structure

```$ tree databases/
databases/
├── component1
│   ├── component1.locale.sql
│   ├── component1.sql
│   ├── component1.ver
│   └── upgrades
│       ├── 1.0_01__1.0_02.sql
│       ├── 1.0_02__1.0_03.sql
│       └── 1.0_03__1.0_04.sql
├── component2
│   ├── component2.locale.sql
│   ├── component2.sql
│   ├── component2.ver
│   └── upgrades
│       ├── 1.0_01__1.0_02.sql
│       ├── 1.0_02__1.0_03.sql
│       └── 1.0_03__1.0_04.sql
└── ersmanual
    ├── ersmanual.locale.sql
    ├── ersmanual.sql
    ├── ersmanual.ver
    └── upgrades
        ├── 1.0_01__1.0_02.sql
        └── 1.0_02__1.0_03.sql
```

## COMMANDS
### Connect
```
python schemainstall.py connectivity -H { DB_NAME/IP }
```


### Schema Install
```
python schemainstall.py install ersmanual -H { DB_NAME/IP }
```


### Apply Locale
```
python schemainstall.py locale ersmanual -H { DB_NAME/IP }
```


### Upgrades
```
python schemainstall.py upgrade ersmanual -H { DB_NAME/IP }
```
