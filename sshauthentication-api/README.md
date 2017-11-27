# SSH authentication API library

The SSH authentication API library provides an interface to using an external authentication provider, such as OpenKeychain, in the SSH authentication layer.

### License
[Apache License v2](https://github.com/open-keychain/open-keychain/blob/master/sshauthentication-api/LICENSE)

### API
For a basic rundown of the API design see the OpenPGP API library's [README.md](https://github.com/open-keychain/openpgp-api/blob/master/README.md).
For a description of the supported methods see [``org.openintents.ssh.authentication.SshAuthenticationApi``](https://github.com/open-keychain/open-keychain/blob/master/sshauthentication-api/src/main/java/org/openintents/ssh/authentication/SshAuthenticationApi.java)

### Add the API library to your project

Add this to your build.gradle:

```gradle
repositories {
    jcenter()
}

dependencies {
    compile 'org.sufficientlysecure:sshauthentication-api:1.0'
}
```

### Build library
1. Go to root dir of OpenKeychain repo
2. Build: ``./gradlew :sshauthentication-api:assemble``
2. Release on bintray: ``./gradlew :sshauthentication-api:bintrayUpload -PbintrayUser=sufficientlysecure -PbintrayKey=INSERT-KEY -PdryRun=false``
