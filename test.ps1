$ErrorActionPreference='Stop'
$out=Join-Path $PSScriptRoot 'build/tests'
New-Item -ItemType Directory -Force $out | Out-Null
& javac -encoding UTF-8 -d $out (Join-Path $PSScriptRoot 'src/local/ivand/pomodoro/TimerEngine.java') (Join-Path $PSScriptRoot 'src/local/ivand/pomodoro/IntervalTransition.java') (Join-Path $PSScriptRoot 'tests/TimerEngineTest.java') (Join-Path $PSScriptRoot 'tests/IntervalTransitionTest.java') (Join-Path $PSScriptRoot 'src/local/ivand/pomodoro/DurationCommand.java') (Join-Path $PSScriptRoot 'tests/DurationCommandTest.java') (Join-Path $PSScriptRoot 'src/local/ivand/pomodoro/TimerDisplay.java') (Join-Path $PSScriptRoot 'tests/TimerDisplayTest.java') (Join-Path $PSScriptRoot 'src/local/ivand/pomodoro/PomodoroPreset.java') (Join-Path $PSScriptRoot 'tests/PomodoroPresetTest.java') (Join-Path $PSScriptRoot 'src/local/ivand/pomodoro/SpokenTime.java') (Join-Path $PSScriptRoot 'tests/SpokenTimeTest.java')
if($LASTEXITCODE -ne 0){throw 'Test compilation failed'}
& java -cp $out TimerEngineTest
if($LASTEXITCODE -ne 0){throw 'Timer tests failed'}
& java -cp $out IntervalTransitionTest
if($LASTEXITCODE -ne 0){throw 'Transition tests failed'}

& java -cp $out DurationCommandTest
if($LASTEXITCODE -ne 0){throw 'Duration command tests failed'}

& java -cp $out TimerDisplayTest
if($LASTEXITCODE -ne 0){throw 'Timer display tests failed'}

& java -cp $out PomodoroPresetTest
if($LASTEXITCODE -ne 0){throw 'Preset tests failed'}

& java -cp $out SpokenTimeTest
if($LASTEXITCODE -ne 0){throw 'Spoken time tests failed'}
