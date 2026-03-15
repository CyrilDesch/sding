rm -rf sding-*
sbt 'clean;Universal/packageBin'
unzip target/universal/sding*
