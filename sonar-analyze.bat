@echo off
echo 🔍 Starting SonarQube Analysis...
mvn sonar:sonar ^
  -Dsonar.host.url=http://localhost:9000 ^
  -Dsonar.login=admin ^
  -Dsonar.password=admin ^
  -DskipITs=true
echo ✅ Analysis submitted! Check http://localhost:9000 for results.
pause
