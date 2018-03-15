package com.opencooffeecamera.sharedPreferences;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Base64;

import com.opencooffeecamera.Common;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

/**
 * Thanks to emmby at http://stackoverflow.com/questions/785973/what-is-the-most-appropriate-way-to-store-user-settings-in-android-application/6393502#6393502
 * 
 * Documentation: http://right-handed-monkey.blogspot.com/2014/04/obscured-shared-preferences-for-android.html
 * This class has the following additions over the original:
 * Additional logic for handling the case for when the preferences were not originally encrypted, but now are.
 * The secret key is no longer hard coded, but defined at runtime based on the individual device.  
 * The benefit is that if one device is compromised, it now only affects that device.
 * 
 * Simply replace your own SharedPreferences object in this one, and any data you read/write will be automatically encrypted and decrypted.
 * 
 * Updated usage:
 *     
 *     ObscuredSharedPreferences obscuredSharedPreferences = ObscuredSharedPreferences.getPrefs(this, MY_APP_NAME, Context.MODE_PRIVATE);
 *     
 *     // To store data:
 *     obscuredSharedPreferences.edit().putString("foo","bar").commit();
 *     
 *     // To get data:
 *     obscuredSharedPreferences.getString("foo", null);     
 *       
 */
public class ObscuredSharedPreferences implements SharedPreferences {
	
	//private static final String LOG_TAG = ObscuredSharedPreferences.class.getSimpleName();
	    
    private static final int ITERATION_COUNT = 20;
    
    private static final String ALGORITHM = "PBEWithMD5AndDES";
    
    // This key is defined at runtime based on ANDROID_ID which is supposed to last the life of the device.
    
    private static char[] SEKRIT = null; 
    private static byte[] SALT = null;
    
    private static char[] backup_secret = null;
    private static byte[] backup_salt = null;

    private SharedPreferences delegate;
    protected Context context;
    private static ObscuredSharedPreferences prefs = null;
    
    // Set to true if a decryption error was detected.
    // In the case of float, int, and long we can tell if there was a parse error.
    // This does not detect an error in strings or boolean - that requires more sophisticated checks.
    private static boolean decryptionErrorFlag = false;
  
    /**
     * Constructor
     * 
     * @param context
     * 
     * @param delegate 
     * SharedPreferences object from the system.
     */
    public ObscuredSharedPreferences(Context context, SharedPreferences delegate) {
        
    	this.delegate = delegate;
        this.context = context;
        
        // Updated thanks to help from bkhall on GitHub.
        ObscuredSharedPreferences.setNewKey(Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID));
        ObscuredSharedPreferences.setNewSalt(Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID));
    }
    
    /**
     * Only used to change to a new key during runtime.
     * If you don't want to use the default per-device key for example.
     * 
     * @param key
     */
    private static void setNewKey(String key) {
    	SEKRIT = key.toCharArray();
    }
    
    /**
     * Only used to change to a new salt during runtime.
     * If you don't want to use the default per-device code for example.
     * 
     * @param salt
     * This must be a string in UTF-8.
     */
    private static void setNewSalt(String salt) {
    	
    	try {
			SALT = salt.getBytes(Common.DEFAULT_CHARSET);
		} catch (UnsupportedEncodingException e) {
	        throw new RuntimeException(e);
		}
    	
    }

    /**
     * Accessor to grab the preferences in a singleton.
     * This stores the reference in a singleton so it can be accessed repeatedly with no performance penalty.
     * 
     * @param c 
     * The context used to access the preferences.
     * 
     * @param appName 
     * Domain the shared preferences should be stored under.
     * 
     * @param contextMode 
     * Typically Context.MODE_PRIVATE.
     * 
     * @return ObscuredSharedPreferences prefs
     */
    public synchronized static ObscuredSharedPreferences getPrefs(Context c, String appName, int contextMode) {
    	
    	if (prefs == null) {
    		
    		// Make sure to use application context since preferences live outside an Activity
    		// use for objects that have global scope like: prefs or starting services.
	    	prefs = new ObscuredSharedPreferences(
	    			c.getApplicationContext(),
	    			c.getApplicationContext().getSharedPreferences(appName, contextMode)
	    			);
    	
    	}
    	
    	return prefs;
    }
    
    public class Editor implements SharedPreferences.Editor {
        
    	SharedPreferences.Editor delegate;

        @SuppressLint("CommitPrefEdits")
        Editor() {
            this.delegate = ObscuredSharedPreferences.this.delegate.edit();                    
        }

        @Override
        public Editor putBoolean(String key, boolean value) {
            delegate.putString(key, encrypt(Boolean.toString(value)));
            return this;
        }

        @Override
        public Editor putFloat(String key, float value) {
            delegate.putString(key, encrypt(Float.toString(value)));
            return this;
        }

        @Override
        public Editor putInt(String key, int value) {
            delegate.putString(key, encrypt(Integer.toString(value)));
            return this;
        }

        @Override
        public Editor putLong(String key, long value) {
            delegate.putString(key, encrypt(Long.toString(value)));
            return this;
        }

        @Override
        public Editor putString(String key, String value) {
            delegate.putString(key, encrypt(value));
            return this;
        }

        @Override
        public void apply() {
        	// To maintain compatibility with android API 7. 
        	delegate.commit();
        }

        @Override
        public Editor clear() {
            delegate.clear();
            return this;
        }

        @Override
        public boolean commit() {
            return delegate.commit();
        }

        @Override
        public Editor remove(String s) {
            delegate.remove(s);
            return this;
        }

		@Override
		public SharedPreferences.Editor putStringSet(String key, Set<String> values) {
			throw new RuntimeException("This class does not work with String Sets.");
		}
		
    }

    public Editor edit() {
        return new Editor();
    }

    @Override
    public Map<String, ?> getAll() {
    	// Left as an exercise to the reader.
    	throw new UnsupportedOperationException(); 
    }
    
    @Override
    public boolean getBoolean(String key, boolean defValue) {
    	
    	// If these weren't encrypted, then it won't be a string.
    	
    	String v;
    	
    	try {
			v = delegate.getString(key, null);
    	} catch (ClassCastException e) {
    		return delegate.getBoolean(key, defValue);
    	}
    	
    	// Boolean string values should be 'true' or 'false'.
    	// Boolean.parseBoolean does not throw a format exception, so check manually.
    	
    	String parsed = decrypt(v);
    	
    	if (!checkBooleanString(parsed)) {
    	
    		// Could not decrypt the Boolean.
    		// Maybe the wrong key was used.
			decryptionErrorFlag = true;
        
			/*
			Log.e(this.getClass().getName(), "Warning, could not decrypt the value. Possible incorrect key used.");
			*/
    	
    	}
        
    	return v != null ? Boolean.parseBoolean(parsed) : defValue;
    }

    /**
     * This function checks if a valid string is received on a request for a Boolean object.
     * 
     * @param string
     * 
     * @return boolean value
     */
    private boolean checkBooleanString(String string) {
    	return (Common.TRUE.equalsIgnoreCase(string) || Common.FALSE.equalsIgnoreCase(string));
    }

    @Override
    public float getFloat(String key, float defValue) {
    	
    	String v;
    	
    	try {
			v = delegate.getString(key, null);
    	} catch (ClassCastException e) {
    		return delegate.getFloat(key, defValue);
    	}
    	
    	try {
			return Float.parseFloat(decrypt(v));
		} catch (NumberFormatException e) {
	
			// Could not decrypt the number.
			// Maybe we are using the wrong key?
			decryptionErrorFlag = true;
        	
			/*
			Log.e(LOG_TAG, "Warning, could not decrypt the value. Possible incorrect key. " + e.getMessage());
			*/
		}
    	
    	return defValue;    
    }

    @Override
    public int getInt(String key, int defValue) {
    	
    	String v;
    	
    	try {
			v = delegate.getString(key, null);
    	} catch (ClassCastException e) {
    		return delegate.getInt(key, defValue);
    	}
    	
    	try {
			return Integer.parseInt(decrypt(v));
		} catch (NumberFormatException e) {
			
			// Could not decrypt the number.
			// Maybe we are using the wrong key?
			decryptionErrorFlag = true;
        	
			/*
			Log.e(LOG_TAG, "Warning, could not decrypt the value. Possible incorrect key. " + e.getMessage());
			*/
		}
    	
    	return defValue;
    }

    @Override
    public long getLong(String key, long defValue) {
    	
    	String v;
    
    	try {
			v = delegate.getString(key, null);
    	} catch (ClassCastException e) {
    		return delegate.getLong(key, defValue);
    	}
    	
    	try {
			return Long.parseLong(decrypt(v));
		} catch (NumberFormatException e) {
			
			// Could not decrypt the number.
			// Maybe we are using the wrong key?
			decryptionErrorFlag = true;
        	
			/*
			Log.e(LOG_TAG, "Warning, could not decrypt the value. Possible incorrect key. " + e.getMessage());
			*/
		}
    	
    	return defValue;
    }

    @Override
    public String getString(String key, String defValue) {
        
    	final String value = delegate.getString(key, null);
        
        return value != null ? decrypt(value) : defValue;
    }

    @Override
    public boolean contains(String s) {
        return delegate.contains(s);
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener onSharedPreferenceChangeListener) {
        delegate.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener onSharedPreferenceChangeListener) {
        delegate.unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    }

	@Override
	public Set<String> getStringSet(String key, Set<String> defValues) {
		throw new RuntimeException("This class does not work with String Sets.");
	}

	/**
	 * Push key allows you to hold the current key being used into a holding location so that it can be retrieved later.
	 * The use case is for when you need to load a new key, but still want to restore the old one.
	 */
	public static void pushKey() {
		backup_secret = SEKRIT;
	}
	
	/**
	 * This takes the key previously saved by pushKey() and activates it as the current decryption key.
	 */
	public static void popKey() {
		SEKRIT = backup_secret;
	}
	
	/**
	 * pushSalt() allows you to hold the current salt being used into a holding location so that it can be retrieved later.
	 * The use case is for when you need to load a new salt, but still want to restore the old one.
	 */
	public static void pushSalt() {
		backup_salt = SALT;
	}
	
	/**
	 * This takes the value previously saved by pushSalt() and activates it as the current salt.
	 */
	public static void popSalt() {
		SALT = backup_salt;
	}
	
    private String encrypt( String value ) {

        try {
            
        	final byte[] bytes = value!=null ? value.getBytes(Common.DEFAULT_CHARSET) : new byte[0];
            
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(ALGORITHM);
            
            SecretKey key = keyFactory.generateSecret(new PBEKeySpec(SEKRIT));
            
            Cipher pbeCipher = Cipher.getInstance(ALGORITHM);
            
            pbeCipher.init(Cipher.ENCRYPT_MODE, key, new PBEParameterSpec(SALT, ITERATION_COUNT));
            
            return new String(Base64.encode(pbeCipher.doFinal(bytes), Base64.NO_WRAP), Common.DEFAULT_CHARSET);
        
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private String decrypt(String value) {
    	
        try {
        	
            final byte[] bytes = value != null ? Base64.decode(value,Base64.DEFAULT) : new byte[0];
            
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(ALGORITHM);
            
            SecretKey key = keyFactory.generateSecret(new PBEKeySpec(SEKRIT));
            
            Cipher pbeCipher = Cipher.getInstance(ALGORITHM);            
            
            pbeCipher.init(Cipher.DECRYPT_MODE, key, new PBEParameterSpec(SALT, ITERATION_COUNT));
            
            return new String(pbeCipher.doFinal(bytes), Common.DEFAULT_CHARSET);
        
        } catch (Exception e) {
        	
        	/*
        	Log.e(LOG_TAG, "Warning, could not decrypt the value. It may be stored in plaintext. " + e.getMessage());
        	*/
        	
        	return value;
        }
    
    }

}