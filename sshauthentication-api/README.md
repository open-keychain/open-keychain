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
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.open-keychain.open-keychain:sshauthentication-api:v5.7.1'
}
```
