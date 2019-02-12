# gradle-slack-liveupdate

[![latest jitpack build](https://jitpack.io/v/warlordofmars/gradle-slack-liveupdate.svg)](https://jitpack.io/#warlordofmars/gradle-slack-liveupdate)

![slack screenshot](https://i.imgur.com/AmrJprI.gif)

## Overview

Gradle Plugin to provide build status updates to Slack in real-time as Gradle executes through tasks

## Setup

To use this plugin, the following buildscript repositories and dependencies must be configured:

```gradle
buildscript {
  repositories {
    maven { url 'https://jitpack.io' }
  }
  dependencies {
    classpath 'com.github.warlordofmars:gradle-slack-liveupdate:release-0.3.12'
  }
}
```

Then to apply the plugin:

```gradle
apply plugin: 'com.github.warlordofmars.gradle.slack'
```

To configure:

```gradle
slack {

    // the slack channel to update during build
    channel '<slack_channel>'

    // slack api token (OAuth or Bot) with appropriate permissions
    token '<slack_api_token>'

    // flag for enabling slack functionality
    // (useful for disabling for local builds)
    enabled true

}
```

## Example Config

Here is an example of configuration that gets the Slack token out from a property value and only enables slack functionality during Jenkins builds:

```gradle
slack {
  channel 'jenkins'
  enabled System.env.containsKey('JENKINS_URL')
  token getProperty('slack.token)
}
```

## Versioning

Versioning on this project is applied automatically on all changes using the [axion-release-plugin](https://github.com/allegro/axion-release-plugin).  Git tags are created for all released versions, and all available released versions can be viewed in the [Releases](https://github.com/warlordofmars/gradle-slack-liveupdate/releases) section of this project.

## Author

* **John Carter** - [warlordofmars](https://github.com/warlordofmars)

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details

## Acknowledgments

* Originally based off of [gradle-slack-plugin](https://github.com/Mindera/gradle-slack-plugin) by [Mindera](https://github.com/Mindera)
* Using [jslack](https://github.com/seratch/jslack) library for Slack API functionality
* Using [grgit](https://github.com/ajoberstar/grgit) library for Git branch / commit / author metadata in Slack updates