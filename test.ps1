$ErrorActionPreference='Stop'
$out=Join-Path $PSScriptRoot 'build/tests'
New-Item -ItemType Directory -Force $out | Out-Null
& javac -encoding UTF-8 -d $out (Join-Path $PSScriptRoot 'src/local/ivand/pomodoro/TimerEngine.java') (Join-Path $PSScriptRoot 'tests/TimerEngineTest.java')
if($LASTEXITCODE -ne 0){throw 'Test compilation failed'}
& java -cp $out TimerEngineTest
if($LASTEXITCODE -ne 0){throw 'Timer tests failed'}
