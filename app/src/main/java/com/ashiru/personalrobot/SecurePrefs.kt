package com.ashiru.personalrobot

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object SecurePrefs {
    private const val PREFS="ai_secure_config"
    private const val KEY_ALIAS="ashiru_ai_api_key_v1"
    private const val VALUE="encrypted_api_key"

    private fun key():SecretKey {
        val store=KeyStore.getInstance("AndroidKeyStore").apply{load(null)}
        val existing=store.getKey(KEY_ALIAS,null) as? SecretKey
        if(existing!=null)return existing
        val generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(KEY_ALIAS,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setRandomizedEncryptionRequired(true).build())
        return generator.generateKey()
    }

    fun save(context:Context,plain:String) {
        if(plain.isBlank()){clear(context);return}
        val cipher=Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE,key())
        val encrypted=cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val packed=cipher.iv+encrypted
        context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(VALUE,Base64.encodeToString(packed,Base64.NO_WRAP)).apply()
    }

    fun read(context:Context):String? {
        val packed=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(VALUE,null)?:return null
        return try{
            val bytes=Base64.decode(packed,Base64.NO_WRAP)
            val iv=bytes.copyOfRange(0,12)
            val ciphertext=bytes.copyOfRange(12,bytes.size)
            val cipher=Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,iv))
            String(cipher.doFinal(ciphertext),Charsets.UTF_8)
        }catch(_:Exception){null}
    }

    fun clear(context:Context){context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().remove(VALUE).apply()}
}