package gatorbytes.utils.nss;

import org.apache.commons.codec.digest.DigestUtils;

import java.math.BigInteger;

public class ComputeRSAModulusDigest {

    private static byte[] convertSignedBytesToUnsigned(byte[] signed) {
        byte[] unsigned = new byte[signed.length - 1];

        System.arraycopy(signed, 1, unsigned, 0, unsigned.length);

        return unsigned;
    }

    public static void main(String[] args) {
        // Replace with your key hash from certutil -K
        String expected = "671e48560e017a5b2bfbb9400f67bbd8f25ab1d3";

        // Replace with your RSA modulus
        String modulusString = "9AB914B935CDF3C691080A80C4F1FE28BE8CAE05795F684EA271A3836932D3C453CEB31B89FC9D29FCBA3942D8AA22CC0DB9CB067693BB355D6583707F5B62F00BADDBEFE46D94BEE4A1FA6CDFDF8D0F64045148A2893CD34EB877C2CA6D9BE1D3E4EDF3F210F794ECF60E0BE142ED6860A90959EED4D4255C0FCF1DD6C421946D84147B409E38CC2655764625A118ABF888603C8EB24EA89C04E3D9D0F83AE7E4A47D1C449BE34EEC92A55D851AF77B6C3F1ED464788F5D08325CADC42E4FA0E767CBC3FD8941FEA7605899132D5BD5238A39EB06F31A980E8298C300A33CC5D56AB084B2223EE31051568C586818F8989DABC5BCEF39B8DD1B2C764DEC7737";

        // Replace with your RSA modulus radix, probably base 10 or 16
        int modulusRadix = 16;

        BigInteger modulus = new BigInteger(modulusString, modulusRadix);

        byte[] unsigned = convertSignedBytesToUnsigned(modulus.toByteArray());

        System.out.println("Expected: " + expected);
        System.out.println("  Actual: " + DigestUtils.sha1Hex(unsigned));
    }

}