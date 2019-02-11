gradle-slack-liveupdate
=======================

[![](https://jitpack.io/v/warlordofmars/gradle-slack-liveupdate.svg)](https://jitpack.io/#warlordofmars/gradle-slack-liveupdate)

![](https://i.imgur.com/AmrJprI.gif)

Overview
--------
Gradle Plugin to provide build status updates to Slack in real-time as Gradle executes through tasks

Setup
-----

To use this plugin, the following buildscript repositories and dependencies must be configured:

```
buildscript {
  repositories {
    maven { url 'https://jitpack.io' }
  }
  dependencies {
    classpath 'com.github.warlordofmars:gradle-cloudformation-helper:release-0.3.9'
  }
}
```

Then to apply the plugin:

```
apply plugin: 'com.github.warlordofmars.gradle.slack'
```

To configure:

```
slack {
    channel '<slack_channel>'
    token '<slack_api_token>'
}
```
