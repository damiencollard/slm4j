# STS - Simple Text Signing library

[STS](https://github.com/damiencollard/slm4j) started as a fork of
[slm4j](http://github.com/starschema/slm4j/).
It is now rewritten in Scala, has a new API and the "license" part has been removed:
the intended purpose of STS is simply to sign text and verify a signed text,
regardless of what the text is.

The signing uses the DSA algorithm.

## Building

Build the library using SBT:

    $ sbt compile
    $ sbt package

## Command line tool

First, create the wrapper script `bin/sts.sh` with SBT by issuing command `sbt mksh`.

Get usage help with option `--help`:

    $ bin/sts.sh --help

To create a private/public key pair in files `test1` and `test1.pub`, respectively:

    $ bin/sts.sh genkeys --base-name test1

To sign file `unsigned.txt` using private key `test1` and write the result to `signed.txt`.

    $ bin/sts.sh sign --private-key test1 --input unsigned.txt --output signed.txt

To verify that signed file `signed.txt` is valid (*i.e.*, has not been tampered with):

    $ bin/sts.sh verify --public-key test1.pub --input signed.txt
    License is valid.
    $ echo $?
    0

Let's check what happens if we modify a signed file:

    $ cp signed.txt tamperedWith.txt
    $ vim tamperedWith.txt
    $ bin/sts.sh verify --public-key test1.pub --input tamperedWith.txt
    License is NOT valid.
    $ echo $?
    2

## Example signed file

As an example, here's a license file that was signed by STS:

    ----- BEGIN TEXT -----
    RegistredTo=John Smith
    ExpirationDate=20090630
    Version=Full
    ----- END TEXT -----
    ----- BEGIN SIGNATURE -----
    MCwCFCKRoTnYFdE7JJzH
    W2XQddSq9wqCR43hRQ+J
    BZV5FS+ZU5j90JAZFUA2
    WQ==
    ----- END SIGNATURE -----

Note that STS imposes no restrictions on the text except that it *must not*
contain any line starting with the delimiter marker `-----`.

Any changes to the text (or the signature) and the validator will consider
the file invalid.

[STS on GitHub](https://github.com/damiencollard/slm4j).

