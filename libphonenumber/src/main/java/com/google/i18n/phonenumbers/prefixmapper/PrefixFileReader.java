/*
 * Copyright (C) 2011 The Libphonenumber Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.i18n.phonenumbers.prefixmapper;

import android.content.Context;

import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A helper class doing file handling and lookup of phone number prefix mappings.
 *
 * @author Shaopeng Jia
 */
public class PrefixFileReader {
  private static final Logger LOGGER = Logger.getLogger(PrefixFileReader.class.getName());

  private final String phonePrefixDataDirectory;
  // The mappingFileProvider knows for which combination of countryCallingCode and language a phone
  // prefix mapping file is available in the file system, so that a file can be loaded when needed.
  private MappingFileProvider mappingFileProvider = new MappingFileProvider();
  // A mapping from countryCallingCode_lang to the corresponding phone prefix map that has been
  // loaded.
  private Map<String, PhonePrefixMap> availablePhonePrefixMaps =
      new HashMap<String, PhonePrefixMap>();

  public PrefixFileReader(String phonePrefixDataDirectory) {
    this.phonePrefixDataDirectory = phonePrefixDataDirectory;
    loadMappingFileProvider();
  }

  public PrefixFileReader(String phonePrefixDataDirectory, Context ctx) throws IOException {
    this.phonePrefixDataDirectory = phonePrefixDataDirectory;
    loadMappingFileProvider(ctx);
  }

  private void loadMappingFileProvider() {
    InputStream source =
        PrefixFileReader.class.getResourceAsStream(phonePrefixDataDirectory + "config");


    ObjectInputStream in = null;
    try {
      in = new ObjectInputStream(source);
      mappingFileProvider.readExternal(in);
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, e.toString());
    } finally {
      close(in);
    }
  }

  private void loadMappingFileProvider(Context ctx) throws IOException {


    InputStream source = ctx.getResources().getAssets().open("data/config");

    ObjectInputStream in = null;
    try {
      in = new ObjectInputStream(source);
      mappingFileProvider.readExternal(in);
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, e.toString());
    } finally {
      close(in);
    }
  }

  private PhonePrefixMap getPhonePrefixDescriptions(
      int prefixMapKey, String language, String script, String region) {
    String fileName = mappingFileProvider.getFileName(prefixMapKey, language, script, region);
    if (fileName.length() == 0) {
      return null;
    }
    if (!availablePhonePrefixMaps.containsKey(fileName)) {
      loadPhonePrefixMapFromFile(fileName);
    }
    return availablePhonePrefixMaps.get(fileName);
  }

  private PhonePrefixMap getPhonePrefixDescriptions(
          int prefixMapKey, String language, String script, String region, Context ctx) {
    String fileName = mappingFileProvider.getFileName(prefixMapKey, language, script, region);
    if (fileName.length() == 0) {
      return null;
    }
    if (!availablePhonePrefixMaps.containsKey(fileName)) {
      loadPhonePrefixMapFromFile(fileName, ctx);
    }
    return availablePhonePrefixMaps.get(fileName);
  }

  private void loadPhonePrefixMapFromFile(String fileName, Context ctx) {
//    InputStream source =
//            PrefixFileReader.class.getResourceAsStream(phonePrefixDataDirectory + fileName);
    InputStream source = null;
    try {
      source = ctx.getResources().getAssets().open("data/"+fileName);
    } catch (IOException e) {
      e.printStackTrace();
    }

    ObjectInputStream in = null;
    try {
      in = new ObjectInputStream(source);
      PhonePrefixMap map = new PhonePrefixMap();
      map.readExternal(in);
      availablePhonePrefixMaps.put(fileName, map);
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, e.toString());
    } finally {
      close(in);
    }
  }
  private void loadPhonePrefixMapFromFile(String fileName) {
    InputStream source =
        PrefixFileReader.class.getResourceAsStream(phonePrefixDataDirectory + fileName);


    ObjectInputStream in = null;
    try {
      in = new ObjectInputStream(source);
      PhonePrefixMap map = new PhonePrefixMap();
      map.readExternal(in);
      availablePhonePrefixMaps.put(fileName, map);
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, e.toString());
    } finally {
      close(in);
    }
  }

  private static void close(InputStream in) {
    if (in != null) {
      try {
        in.close();
      } catch (IOException e) {
        LOGGER.log(Level.WARNING, e.toString());
      }
    }
  }

  /**
   * Returns a text description in the given language for the given phone number.
   *
   * @param number  the phone number for which we want to get a text description
   * @param lang  two-letter lowercase ISO language codes as defined by ISO 639-1
   * @param script  four-letter titlecase (the first letter is uppercase and the rest of the letters
   *     are lowercase) ISO script codes as defined in ISO 15924
   * @param region  two-letter uppercase ISO country codes as defined by ISO 3166-1
   * @return  a text description in the given language for the given phone number, or an empty
   *     string if a description is not available
   */
  public String getDescriptionForNumber(
      PhoneNumber number, String lang, String script, String region) {
    int countryCallingCode = number.getCountryCode();
    // As the NANPA data is split into multiple files covering 3-digit areas, use a phone number
    // prefix of 4 digits for NANPA instead, e.g. 1650.
    int phonePrefix = (countryCallingCode != 1) ?
        countryCallingCode : (1000 + (int) (number.getNationalNumber() / 10000000));
    PhonePrefixMap phonePrefixDescriptions =
        getPhonePrefixDescriptions(phonePrefix, lang, script, region);
    String description = (phonePrefixDescriptions != null) ?
        phonePrefixDescriptions.lookup(number) : null;
    // When a location is not available in the requested language, fall back to English.
    if ((description == null || description.length() == 0) && mayFallBackToEnglish(lang)) {
      PhonePrefixMap defaultMap = getPhonePrefixDescriptions(phonePrefix, "en", "", "");
      if (defaultMap == null) {
        return "";
      }
      description = defaultMap.lookup(number);
    }
    return description != null ? description : "";
  }

  public String getDescriptionForNumber(
          PhoneNumber number, String lang, String script, String region, Context ctx) {
    int countryCallingCode = number.getCountryCode();
    // As the NANPA data is split into multiple files covering 3-digit areas, use a phone number
    // prefix of 4 digits for NANPA instead, e.g. 1650.
    int phonePrefix = (countryCallingCode != 1) ?
            countryCallingCode : (1000 + (int) (number.getNationalNumber() / 10000000));
    PhonePrefixMap phonePrefixDescriptions =
            getPhonePrefixDescriptions(phonePrefix, lang, script, region, ctx);
    String description = (phonePrefixDescriptions != null) ?
            phonePrefixDescriptions.lookup(number) : null;
    // When a location is not available in the requested language, fall back to English.
    if ((description == null || description.length() == 0) && mayFallBackToEnglish(lang)) {
      PhonePrefixMap defaultMap = getPhonePrefixDescriptions(phonePrefix, "en", "", "", ctx);
      if (defaultMap == null) {
        return "";
      }
      description = defaultMap.lookup(number);
    }
    return description != null ? description : "";
  }

  private boolean mayFallBackToEnglish(String lang) {
    // Don't fall back to English if the requested language is among the following:
    // - Chinese
    // - Japanese
    // - Korean
    return !lang.equals("zh") && !lang.equals("ja") && !lang.equals("ko");
  }
}
