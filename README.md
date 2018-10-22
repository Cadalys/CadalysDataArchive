# CadalysDataArchive1
[![Deploy](https://www.herokucdn.com/deploy/button.svg)](https://heroku.com/deploy)


# copying heroku db to local machine:
heroku pg:backups:capture
heroku pg:backups:download
pg_restore --verbose --clean --no-acl --no-owner -h localhost -U postgres -d chasdev latest.dump