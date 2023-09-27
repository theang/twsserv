#include "kyber1024_pke.hpp"
#include <cassert>
#include <iostream>

void writef(void *data, size_t len, char *fname) {
    FILE* f = fopen(fname, "wb");
    fwrite(data, 1, len, f);
    fclose(f);
}

void help() {
    std::cout << "Nothing to do (usage: -g <pubkey_output_file> <privkey_output_file> to gen keys\n"
                 "                      -e <file_to_encrypt> <pubkey_file> <output_file> to encrypt\n"
                 "                      -d <file_to_decrypt> <private_key> <output_file> to decrypt" << "\n" ;
}
// Compile it with
//
// g++ -std=c++20 -Wall -O3 -march=native -I ./include -I ./sha3/include
// based on example/kyber512_pke.cpp
// check https://github.com/itzmeanjan/kyber/tree/master for dependencies
int
main(int argc, char *argv[])
{
    if (argc < 2) {
        help();
        exit(1);
    }
  
    constexpr size_t pklen = kyber1024_pke::pub_key_len();
    constexpr size_t sklen = kyber1024_pke::sec_key_len();
    constexpr size_t enclen = kyber1024_pke::cipher_text_len();
    constexpr size_t mlen = 32;
    constexpr size_t slen = 1024; // = N bytes
    uint8_t seed[slen];
    FILE *frnd = fopen("/dev/random", "rb");
    fread(seed, 1, slen, frnd);
    fclose(frnd);
    prng::prng_t prng{seed, slen};
  
    if (!strcmp(argv[1], "-g") || argc < 4) {
        std::cout << "Key generation\n";
  
        uint8_t* pubkey = static_cast<uint8_t*>(std::malloc(pklen));
        uint8_t* seckey = static_cast<uint8_t*>(std::malloc(sklen));
        std::memset(pubkey, 0, pklen);
        std::memset(seckey, 0, sklen);
  
        kyber1024_pke::keygen(prng, pubkey, seckey);
  
        writef(pubkey, pklen, argv[2]);
        writef(seckey, sklen, argv[3]);
    } else if (!strcmp(argv[1], "-e") || argc < 5) {
        uint8_t* pubkey = static_cast<uint8_t*>(std::malloc(pklen));
        FILE *finkey = fopen(argv[3], "rb");
        fread(pubkey, 1, pklen, finkey);
        fclose(finkey);
        FILE *fin = fopen(argv[2], "rb");
        fseek(fin, 0l, SEEK_END);
        long size = ftell(fin);
        fseek(fin, 0l, SEEK_SET);
        uint8_t* data = static_cast<uint8_t*>(std::malloc(size));
        uint8_t* enc_data = static_cast<uint8_t*>(std::malloc(size/mlen*enclen));
        uint8_t* rcoin = static_cast<uint8_t*>(std::malloc(mlen));
        if (size % mlen != 0) {
            std::cout << "size of file to encrypt should be devisible by " << mlen;
            exit(1);
        }
        fread(data, 1, size, fin);
        fclose(fin);
  
        for (unsigned long i = 0; i < size / mlen; i++) {
           prng.read(rcoin, mlen);
           kyber1024_pke::encrypt(pubkey, data+mlen*i, rcoin, enc_data+enclen*i);
        }
        FILE *fout = fopen(argv[4],"wb");
        fwrite(enc_data, 1, size/mlen*enclen, fout);
        fclose(fout);
    } else if (!strcmp(argv[1], "-d") || argc < 5) {
        uint8_t* seckey = static_cast<uint8_t*>(std::malloc(sklen));
        FILE *finkey = fopen(argv[3], "rb");
        fread(seckey, 1, sklen, finkey);
        fclose(finkey);
        FILE *fin = fopen(argv[2], "rb");
        fseek(fin, 0l, SEEK_END);
        long size = ftell(fin);
        fseek(fin, 0l, SEEK_SET);
        size_t msize = size / enclen * mlen;
        uint8_t* data = static_cast<uint8_t*>(std::malloc(size));
        uint8_t* dec_data = static_cast<uint8_t*>(std::malloc(msize));
        if (size % enclen != 0) {
            std::cout << "size of file to decrypt should be divisible by " << enclen ;
            exit(1);
        }
        fread(data, 1, size, fin);
        fclose(fin);
  
        for (unsigned long i = 0; i < size / enclen; i++) {
           kyber1024_pke::decrypt(seckey, data+enclen*i, dec_data+mlen*i);
        }
        FILE *fout = fopen(argv[4],"wb");
        fwrite(dec_data, 1, msize, fout);
        fclose(fout);
    } else {
        std::cout << "Wrong usage\n";
        help();
        exit(1);
    }
  
    return EXIT_SUCCESS;
}
