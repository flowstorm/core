@echo off
$JAVA_HOME/bin/javapackager -deploy -native msi -name Promethist -BappVersion=2.0.0 -srcdir target -srcfiles promethist.jar -appclass ai.promethist.standalone.Application -outdir target -outfile p -v
