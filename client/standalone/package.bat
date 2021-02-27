@echo off
$JAVA_HOME/bin/javapackager -deploy -native msi -name Flowstorm -BappVersion=2.0.0 -srcdir target -srcfiles flowstorm.jar -appclass ai.flowstorm.standalone.Application -outdir target -outfile p -v
