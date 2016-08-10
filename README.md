# AndroidPatchPlugin
android patch plugin
Usage
====
Please refer to sample module.

Versioning
====
Please refer to semver [standard](http://semver.org)

Note
====
To release a new generator plugin, following steps will be needed
* Add buildSrc to settings.gradle
* Update plugin version
* ./gradlew clean buildSrc:bintrayUpload
* After upload, discard changes in settings.gradle
* Commit other changes

