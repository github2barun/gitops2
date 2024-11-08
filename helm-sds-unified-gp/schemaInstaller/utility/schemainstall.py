#!/usr/bin/env python3

import sys
import os
import pymysql
import datetime
import re
import subprocess
import configparser
from io import StringIO
import getopt

## VARS
db_user = "root"
db_pass = "refill"
databases_dir = "/schema-install/databases"
dbs_list = '/schema-install/schemas.ini'

success_message = "Host: {}\nPort: {}\nStatus: Success"
failure_message = "Host: {}\nPort: {}\nStatus: Failed"

def display_usage():
    usage_message = '''Usage: dbinstall [options] [APP_NAME]
Options:
    connectivity   check database connectivity
    install        install databases
    locale         apply locale
    upgrade        upgrade database'''
    print(usage_message)

# Helper function to ensure that a correct port number is passed. Defaults to 3306
def port_check(port):
    try:
        if port:
            port = int(port)
            if port < 1 or port > 65536:
                print('Port must be between 1 and 65536. Setting it to 3306')
                port = 3306
    except TypeError:
        print('Port must be a number. Defaulting to 3306')
        port = 3306
    return port

def check_connectivity(database_host='localhost', port=3306):
    port = port_check(port)
    db = None
    try:
        db = pymysql.connect(host=database_host, port=port, user=db_user, passwd=db_pass)
        print(success_message.format(database_host, port))
    except pymysql.Error as e:
        print(failure_message.format(database_host, port))
    finally:
        if db is not None:
            db.close()

def install_database(app_name, database_host='localhost', port=3306):
    # Read and parse the dbs.ini file
    config = configparser.ConfigParser()
    try:
        config.read(dbs_list)
    except configparser.Error as e:
        print("Error reading configuration file: {}".format(e))
        return

    # Get the list of schemas from both [transaction] and [reporting] sections
    transaction_dbs = config.get('transaction', 'dbs').split(',')
    reporting_dbs = config.get('reporting', 'dbs').split(',')

    # Strip whitespace from each schema name
    transaction_dbs = [db.strip() for db in transaction_dbs]
    reporting_dbs = [db.strip() for db in reporting_dbs]

    # Check if the app_name exists in either [transaction] or [reporting] sections
    if app_name in transaction_dbs or app_name in reporting_dbs:
        print("Installing database for '{}'...".format(app_name))
    else:
        print("Schema name '{}' not found in config file".format(app_name))
        return

    port = port_check(port)
    cursor = None
    db = None
    try:
        db = pymysql.connect(host=database_host, port=port, user=db_user, passwd=db_pass)
        cursor = db.cursor()

        # Path to the SQL file
        sql_file = os.path.join(databases_dir, app_name, app_name + ".sql")

        # Check if the SQL file exists
        if not os.path.isfile(sql_file):
            print("Schema file for {} not found".format(app_name))
            return

        # Read the SQL file and extract the schema name
        with open(sql_file, 'r') as file:
            sql_script = file.read()

        # Use regex to find the schema name
        match = re.search(r'CREATE\s+DATABASE\s+IF\s+NOT\s+EXISTS\s+`?(\w+)`?(?:\s+DEFAULT\s+CHARACTER\s+SET\s+\w+)?;', sql_script, re.IGNORECASE | re.DOTALL)
        if match:
            schema_name = match.group(1)
        else:
            print("No valid CREATE DATABASE statement found in {}".format(sql_file))
            return

        # Check if the schema_name database exists
        check_db_query = "SHOW DATABASES LIKE '{}';".format(schema_name)
        cursor.execute(check_db_query)
        result = cursor.fetchone()

        if result:
            print("Database '{}' already exists.".format(schema_name))
            reinstall = input("Do you want to reinstall the database? (y/n): ")

            if reinstall.lower() == 'y':
                print("Dropping the existing database '{}'...".format(schema_name))
                # Drop the database using SQL
                drop_db_query = "DROP DATABASE IF EXISTS `{}`;".format(schema_name)
                cursor.execute(drop_db_query)
                print("Database '{}' dropped.".format(schema_name))
            else:
                print("Schema installation aborted.")
                return

        # Execute the SQL script, which should contain CREATE and USE database statements
        statements = sql_script.split(';')
        statements = [statement.strip() for statement in statements if statement.strip()]

        # Disable the warnings for unknown tables
        cursor.execute("SET sql_notes = 0")

        # Execute each statement in the SQL file
        for statement in statements:
            cursor.execute(statement)

        # Re-enable the warnings
        cursor.execute("SET sql_notes = 1")

        # Read the version from the file
        version_file = os.path.join(databases_dir, app_name, app_name + ".ver")
        with open(version_file, 'r') as file:
            version = file.read().strip()

        # Prepare the insert query parameters
        select_query = "SELECT COUNT(*) FROM {}.ersinstall".format(schema_name)
        cursor.execute(select_query)
        version_key = cursor.fetchone()[0] + 1
        status = 1
        script = "Fresh installation"
        last_modified = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")

        # Construct the insert query
        insert_query = "INSERT INTO {}.ersinstall (VersionKey, Version, Status, Script, last_modified) VALUES ({}, '{}', {}, '{}', '{}')".format(
            schema_name, version_key, version, status, script, last_modified
        )

        # Execute the insert query
        cursor.execute(insert_query)

        # Commit the changes
        db.commit()

        print("Schema for {} successfully installed".format(app_name))

    except pymysql.Error as e:
        print("Schema for {} failed: {}".format(app_name, e))
    finally:
        # Close the cursor and the database connection
        if cursor is not None:
            cursor.close()
        if db is not None:
            db.close()

def install_all_tr_databases(dbs_list, database_host='localhost', database_port=3306):
    try:
        with open(dbs_list, 'r') as file:
            contents = file.read()

        database_list = []
        config = configparser.ConfigParser()
        config.read_file(StringIO(contents))

        if config.has_section('transaction'):
            dbs = config.get('transaction', 'dbs')
            database_list = [db.strip() for db in dbs.split(',')]

        for app_name in database_list:
            install_database(app_name, database_host, database_port)

    except OSError as e:
        print("Error occurred while installing databases:", e)

def install_all_rp_databases(dbs_list, database_host='localhost', database_port=3306):
    try:
        with open(dbs_list, 'r') as file:
            contents = file.read()

        database_list = []
        config = configparser.ConfigParser()
        config.read_file(StringIO(contents))

        if config.has_section('reporting'):
            dbs = config.get('reporting', 'dbs')
            database_list = [db.strip() for db in dbs.split(',')]

        for app_name in database_list:
            install_database(app_name, database_host, database_port)

    except OSError as e:
        print("Error occurred while installing databases:", e)

def install_all_databases(database_host='localhost', database_port=3306):
    try:
        install_all_tr_databases(dbs_list, database_host, database_port)
        install_all_rp_databases(dbs_list, database_host, database_port)

    except OSError as e:
        print("Error occurred while installing databases:", e)

def apply_locale(app_name, database_host='localhost', port=3306):
    port = port_check(port)
    config = configparser.ConfigParser()
    try:
        config.read(dbs_list)
    except configparser.Error as e:
        print("Error reading configuration file: {}".format(e))
        return

    # Check if the schema name exists in the locales of transaction or reporting
    schema_found = False
    if config.has_section('transaction'):
        transaction_schemas = config.get('transaction', 'locales').split(',')
        if app_name in [db.strip() for db in transaction_schemas]:
            schema_found = True

    if config.has_section('reporting'):
        reporting_schemas = config.get('reporting', 'locales').split(',')
        if app_name in [db.strip() for db in reporting_schemas]:
            schema_found = True

    if not schema_found:
        print("Locale for schema '{}' not found in config file".format(app_name))
        return

    cursor = None
    db = None
    try:
        db = pymysql.connect(host=database_host, port=port, user=db_user, passwd=db_pass)
        cursor = db.cursor()

        # Path to the locale SQL file
        locale_file = os.path.join(databases_dir, app_name, "locale.sql")

        # Check if the locale file exists
        if not os.path.isfile(locale_file):
            print("Locale file for %s not found at %s" % (app_name, locale_file))
            return

        # Read the locale SQL file
        with open(locale_file, 'r') as file:
            sql_script = file.read()

        # Split the SQL script into individual statements
        statements = sql_script.split(';')

        # Remove empty statements
        statements = [statement.strip() for statement in statements if statement.strip()]

        # Execute each statement separately and check for errors
        for statement in statements:
            try:
                cursor.execute(statement)
            except pymysql.Error as e:
                print("Failed to execute statement: %s" % statement)
                print("MySQL Error: %s" % e)
                db.rollback()
                return

        # Commit the transaction
        db.commit()
        print("Locale for %s successfully applied" % app_name)

    except pymysql.Error as e:
        print("Failed to apply locale for %s: %s" % (app_name, e))
    except Exception as e:
        print("An unexpected error occurred: %s" % e)
    finally:
        # Close the cursor and the database connection
        if cursor is not None:
            cursor.close()
        if db is not None:
            db.close()

def apply_all_tr_locales(dbs_list, database_host='localhost', database_port=3306):
    try:
        with open(dbs_list, 'r') as file:
            contents = file.read()

        database_list = []
        config = configparser.ConfigParser()
        config.read_file(StringIO(contents))

        if config.has_section('transaction'):
            dbs = config.get('transaction', 'locales')
            database_list = [db.strip() for db in dbs.split(',')]

        for app_name in database_list:
            apply_locale(app_name, database_host, database_port)

    except OSError as e:
        print("Error occurred while applying locales:", e)

def apply_all_rp_locales(dbs_list, database_host='localhost', database_port=3306):
    try:
        with open(dbs_list, 'r') as file:
            contents = file.read()

        database_list = []
        config = configparser.ConfigParser()
        config.read_file(StringIO(contents))

        if config.has_section('reporting'):
            dbs = config.get('reporting', 'locales')
            database_list = [db.strip() for db in dbs.split(',')]

        for app_name in database_list:
            apply_locale(app_name, database_host, database_port)

    except OSError as e:
        print("Error occurred while applying locales:", e)

def apply_all_locales(database_host='localhost', database_port=3306):
    try:
        apply_all_tr_locales(dbs_list, database_host, database_port)
        apply_all_rp_locales(dbs_list, database_host, database_port)

    except OSError as e:
        print("Error occurred while applying locales:", e)


def upgrade_database(app_name, database_host='localhost', port=3306):
    port = port_check(port)
    config = configparser.ConfigParser()
    try:
        config.read(dbs_list)
    except configparser.Error as e:
        print("Error reading configuration file: {}".format(e))
        return

    # Check if the schema name exists in either the 'transaction' or 'reporting' sections
    schema_found = False
    if config.has_section('transaction'):
        transaction_dbs = config.get('transaction', 'dbs').split(',')
        if app_name in [db.strip() for db in transaction_dbs]:
            schema_found = True

    if config.has_section('reporting'):
        reporting_dbs = config.get('reporting', 'dbs').split(',')
        if app_name in [db.strip() for db in reporting_dbs]:
            schema_found = True

    if not schema_found:
        print("Schema name '{}' not found in config file".format(app_name))
        return

    db = None
    cursor = None
    try:
        db = pymysql.connect(host=database_host, port=int(port) if port else 3306, user=db_user, passwd=db_pass)
        cursor = db.cursor()

        # Use the APP_NAME database
        use_db_query = "USE {};".format(app_name)
        cursor.execute(use_db_query)

        # Fetch the current version from the APP_NAME.ersinstall table
        current_version_query = "SELECT Version FROM {}.ersinstall ORDER BY VersionKey DESC LIMIT 1;".format(app_name)
        cursor.execute(current_version_query)
        current_version = cursor.fetchone()[0]

        # Construct the path to the upgrade files directory for the specified app_name
        upgrade_files_dir = os.path.join(databases_dir, app_name, "upgrades")

        # Fetch all the upgrade file names
        upgrade_files = sorted([f for f in os.listdir(upgrade_files_dir) if os.path.isfile(os.path.join(upgrade_files_dir, f))])

        if not upgrade_files:
            print("No upgrade found for {}".format(app_name))
            return

        # Filter the upgrade files based on the current version
        filtered_upgrade_files = [f for f in upgrade_files if re.match(r'^{}__.*\.sql$'.format(re.escape(current_version)), f)]

        if not filtered_upgrade_files:
            print("No matching upgrade files found for {}".format(app_name))
            return

        # Upgrade the database
        for upgrade_file in filtered_upgrade_files:
            upgrade_file_path = os.path.join(upgrade_files_dir, upgrade_file)
            with open(upgrade_file_path, 'r') as file:
                sql_script = file.read()
            # Split commands by semicolon, making sure to filter out any empty statements
            sql_commands = [cmd.strip() for cmd in sql_script.split(';') if cmd.strip()]
            # Execute each command separately
            for command in sql_commands:
                try:
                    cursor.execute(command)
                except pymysql.Error as e:
                    print("Failed to execute command: {}. Error: {}".format(command, e))

            # Extract the new version from the filename
            new_version_match = re.search(r'^{}__(.*)\.sql$'.format(re.escape(current_version)), upgrade_file)
            if new_version_match:
                new_version = new_version_match.group(1)
                print("New version: " + new_version)

                # Fetch all results from the previous execution
                while cursor.nextset():
                    pass

                # Update the version in the APP_NAME.ersinstall table
                update_version_query = "INSERT INTO {}.ersinstall (Version, Status, Script, last_modified) VALUES ('{}', 2, '{}', '{}')".format(
                    app_name, new_version, upgrade_file_path, datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S"))
                cursor.execute(update_version_query)
                db.commit()
                print("Schema upgrade for {} successful".format(app_name))
            else:
                print("Failed to extract new version from the file: {}".format(upgrade_file))

    except pymysql.Error as e:
        print("Upgrade failed for {}: {}".format(app_name, e))
        print("Failed SQL script: {}".format(sql_script))
    finally:
        # Close the cursor and the database connection
        if cursor is not None:
            cursor.close()
        if db is not None:
            db.close()

def main():
    if len(sys.argv) == 1:
        display_usage()
    elif sys.argv[1] == "install" and sys.argv[2] == "all_tr":
        database_host = 'localhost'
        database_port = 3306

        if len(sys.argv) >= 4:
            if sys.argv[3] == "-H":
                database_host = sys.argv[4]
            elif sys.argv[3] == "-P":
                database_port = int(sys.argv[4])

        install_all_tr_databases(dbs_list, database_host, database_port)
    elif sys.argv[1] == "install" and sys.argv[2] == "all_rp":
        database_host = 'localhost'
        database_port = 3306

        if len(sys.argv) >= 4:
            if sys.argv[3] == "-H":
                database_host = sys.argv[4]
            elif sys.argv[3] == "-P":
                database_port = int(sys.argv[4])

        install_all_rp_databases(dbs_list, database_host, database_port)
    elif sys.argv[1] == "install" and sys.argv[2] == "all":
        database_host = 'localhost'
        database_port = 3306

        if len(sys.argv) >= 4:
            if sys.argv[3] == "-H":
                database_host = sys.argv[4]
            elif sys.argv[3] == "-P":
                database_port = int(sys.argv[4])

            if len(sys.argv) == 6:
                if sys.argv[5] == "-H":
                    database_host = sys.argv[6]
                elif sys.argv[5] == "-P":
                    database_port = int(sys.argv[6])

        install_all_databases(database_host, database_port)
    elif sys.argv[1] == "install":
        app_name = sys.argv[2]
        database_host = 'localhost'
        database_port = 3306

        if len(sys.argv) >= 5:
            if sys.argv[3] == "-H":
                database_host = sys.argv[4]
            elif sys.argv[3] == "-P":
                database_port = int(sys.argv[4])

        install_database(app_name, database_host, database_port)
    elif len(sys.argv) >= 3 and sys.argv[1] == "upgrade":
        app_name = sys.argv[2]
        database_host = 'localhost'
        port = 3306

        if len(sys.argv) >= 5:
            if sys.argv[3] == "-H":
                database_host = sys.argv[4]
            elif sys.argv[3] == "-P":
                port = int(sys.argv[4])

            if len(sys.argv) == 7 and sys.argv[5] == "-P":
                port = int(sys.argv[6])

        upgrade_database(app_name, database_host, port)
    elif len(sys.argv) == 1:
        display_usage()
    elif sys.argv[1] == "locale" and sys.argv[2] == "all_tr":
        database_host = 'localhost'
        database_port = 3306

        if len(sys.argv) >= 4:
            if sys.argv[3] == "-H":
                database_host = sys.argv[4]
            elif sys.argv[3] == "-P":
                database_port = int(sys.argv[4])

        apply_all_tr_locales(dbs_list, database_host, database_port)
    elif sys.argv[1] == "locale" and sys.argv[2] == "all_rp":
        database_host = 'localhost'
        database_port = 3306

        if len(sys.argv) >= 4:
            if sys.argv[3] == "-H":
                database_host = sys.argv[4]
            elif sys.argv[3] == "-P":
                database_port = int(sys.argv[4])

        apply_all_rp_locales(dbs_list, database_host, database_port)
    elif sys.argv[1] == "locale" and sys.argv[2] == "all":
        database_host = 'localhost'
        database_port = 3306

        if len(sys.argv) >= 4:
            if sys.argv[3] == "-H":
                database_host = sys.argv[4]
            elif sys.argv[3] == "-P":
                database_port = int(sys.argv[4])

            if len(sys.argv) == 6:
                if sys.argv[5] == "-H":
                    database_host = sys.argv[6]
                elif sys.argv[5] == "-P":
                    database_port = int(sys.argv[6])

        apply_all_locales(database_host, database_port)
    elif sys.argv[1] == "locale":
        app_name = sys.argv[2]
        database_host = 'localhost'
        database_port = 3306

        if len(sys.argv) >= 5:
            if sys.argv[3] == "-H":
                database_host = sys.argv[4]
            elif sys.argv[3] == "-P":
                database_port = int(sys.argv[4])

        apply_locale(app_name, database_host, database_port)
    elif len(sys.argv) == 2 and sys.argv[1] == "connectivity":
        check_connectivity()
    elif len(sys.argv) == 4 and sys.argv[1] == "connectivity" and sys.argv[2] == "-H":
        database_host = sys.argv[3]
        check_connectivity(database_host)
    elif len(sys.argv) == 4 and sys.argv[1] == "connectivity" and sys.argv[2] == "-P":
        port = int(sys.argv[3])
        check_connectivity(port=port)
    elif len(sys.argv) == 6 and sys.argv[1] == "connectivity" and sys.argv[2] == "-H" and sys.argv[4] == "-P":
        database_host = sys.argv[3]
        port = int(sys.argv[5])
        check_connectivity(database_host, port)
    else:
        display_usage()

if __name__ == "__main__":
    main()