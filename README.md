## About

This is a utility class that can be used to track down an RSA key in an NSS database to its corresponding [CSR](https://en.wikipedia.org/wiki/Certificate_signing_request) or [certificate](https://en.wikipedia.org/wiki/Public_key_certificate) file. See [Background](#background) for more information.

## Running

This project uses [Gradle](https://guides.gradle.org/building-java-applications/#run_the_application). Clone this project, `cd` into the top-level directory and run:
```
$ ./gradlew run
```

## Inputs

Replace `expected` with the hash of your key from the output of `certutil -K` on your NSS database.

Replace `modulusString` with your RSA modulus. You can retrieve the modulus in a variety of ways. For example, use `openssl` on a CSR to get the modulus in base 16:
```
$ openssl req -in example.csr -modulus
Modulus=9AB914B935CDF3C691080A80C4F1FE28BE8CAE05795F684EA271A3836932D3C453CEB31B89FC9D29FCBA3942D8AA22CC0DB9CB067693BB355D6583707F5B62F00BADDBEFE46D94BEE4A1FA6CDFDF8D0F64045148A2893CD34EB877C2CA6D9BE1D3E4EDF3F210F794ECF60E0BE142ED6860A90959EED4D4255C0FCF1DD6C421946D84147B409E38CC2655764625A118ABF888603C8EB24EA89C04E3D9D0F83AE7E4A47D1C449BE34EEC92A55D851AF77B6C3F1ED464788F5D08325CADC42E4FA0E767CBC3FD8941FEA7605899132D5BD5238A39EB06F31A980E8298C300A33CC5D56AB084B2223EE31051568C586818F8989DABC5BCEF39B8DD1B2C764DEC7737
```
Or use Java to parse a certificate for the modulus in base 10:
```
CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
InputStream inputStream = new FileInputStream("/path/to/your/certificate/file.cer");
X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(inputStream);
System.out.println(certificate.getPublicKey().toString());
```
produces:
```
Sun RSA public key, 2048 bits
  modulus: 29010481853899063528684369979232795154415291354547344927444572756776420252588777655249435617990573192785649615958326134691259915008316855562474234976274995386697741110824816315182265885309713368176815023555432004513955844856511714742451975637571084901616832366260238094566985369935004678910863619973538803535579774384275158987380238621368525032774819731111469823712051199958516560087397078914357357082642982602812209391661583741322591957684667112548281332368388594801245967211138040993035824361850480779952394803327178460119468768027524838967955142221515232733799897494132918524595981314240895728084813561029462567581
  public exponent: 65537
```
Whichever method you use, make sure you include the correct radix in `modulusRadix`.

## Background

If you frequently work with NSS databases, you have probably used the `certutil` command and specifically the `-L` and `-K` options to list certificates and keys in the database. When you create a new RSA certificate request with `-R`, NSS generates an RSA keypair and a CSR that can be submitted to a CA. At this time, `certutil -K` will list your key as an orphan. Once your CSR has been submitted to a CA and signed, you will receive an X.509 certificate. After you add the issued certificate to your database with `-A` and provide a nickname with `-n`, you can now see your certificate and key with `-L` and `-K` linked together by the RSA modulus and easily identifiable with the human readable nickname.

This process works, but what happens when the application using your NSS database doesn't use readable nicknames? If you have many CSRs from many different NSS databases, how would you track down which database a CSR originated from? We can solve these problems of tracking CSRs, certificates, and keys around databases with a little knowledge of the internals of NSS.

As mentioned earlier, `certutil -K` lists the keys in an NSS database:
```
$ certutil -K -d .
certutil: Checking token "NSS Certificate DB" in slot "NSS User Private Key and Certificate Services"
Enter Password or Pin for "NSS Certificate DB":
< 0> rsa      671e48560e017a5b2bfbb9400f67bbd8f25ab1d3   (orphan)
```
The output provides a long hex string for each key that looks suspiciously like a hash digest. The 40 character length strongly hints at SHA1. But what is it a digest of?

A look at the [documentation](https://developer.mozilla.org/en-US/docs/Mozilla/Projects/NSS/Tools/certutil) for `certutil -K` provides another hint:

>List the key ID of keys in the key database. A key ID is the **modulus of the RSA key** or the publicValue of the DSA key. IDs are displayed in hexadecimal ("0x" is not shown).

It appears to be a SHA1 hash of the RSA key modulus, but a quick mock up in Java to test this idea yields a different hash value. After reviewing the NSS source, the RSA modulus is often stored in an unsigned long data type. Since there is no direct unsigned equivalent in Java, we'll need to do some extra work. BigInteger provides easy access to the two's complement binary value via `toByteArray()` and the javadoc for `toByteArray()` indicates the array is big-endian. Therefore, all we need to do is drop the first bit (the sign bit) and re-hash. Running the class again with this update confirms that our SHA1 hash of the unsigned RSA modulus is equivalent to the NSS output.
