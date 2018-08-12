package org.osgl.util;

import org.osgl.$;

import java.util.*;

public final class MimeType {

    private static Map<String, MimeType> indexByFileExtension = new HashMap<>();
    private static Map<String, MimeType> indexByContentType = new HashMap<>();
    private static Map<String, Trait> traitMap = new HashMap<>();

    public enum Trait {
        archive, audio, excel, image, pdf, powerpoint, text, video, word;
        public boolean test(MimeType mimeType) {
            return mimeType.test(this);
        }
    }

    private String fileExtension;
    private String type;
    private EnumSet<Trait> traits = EnumSet.noneOf(Trait.class);

    private MimeType(String fileExtension, String type, List<Trait> traitList) {
        this.fileExtension = fileExtension;
        this.type = type;
        this.traits.addAll(traitList);
    }

    @Override
    public String toString() {
        return type;
    }

    public String fileExtension() {
        return fileExtension;
    }

    public String type() {
        return type;
    }

    public boolean test(Trait trait) {
        return traits.contains(trait);
    }

    public boolean test(String s) {
        if (fileExtension.equalsIgnoreCase(s)) {
            return true;
        }
        if (type.equalsIgnoreCase(s)) {
            return true;
        }
        Trait trait = traitMap.get(s);
        return null != trait;
    }

    public static MimeType findByFileExtension(String fileExtension) {
        return indexByFileExtension.get(fileExtension);
    }

    public static MimeType findByContentType(String contentType) {
        return indexByContentType.get(contentType);
    }

    public static Collection<MimeType> allMimeTypes() {
        return indexByFileExtension.values();
    }

    /**
     * Return a content type string corresponding to a given file extension suffix.
     *
     * If there is no MimeType corresponding to the file extension, then returns the file
     * extension string directly.
     *
     * @param fileExtension
     *      file extension suffix
     * @return
     *      A content type string corresponding to the file extension suffix
     *      or the file extension suffix itself if no corresponding mimetype found.
     */
    public static String typeOfSuffix(String fileExtension) {
        MimeType mimeType = indexByFileExtension.get(fileExtension);
        return null == mimeType ? fileExtension : mimeType.type;
    }

    static {
        init();
    }

    private static void init() {
        for (Trait trait : traitMap.values()) {
            traitMap.put(trait.name(), trait);
        }
        List<String> lines = IO.read(MimeType.class.getResource("/org/osgl/mime-types2.properties")).toLines();
        for (String line : lines) {
            S.Pair pair = S.binarySplit(line, '=');
            String fileExtension = pair.left();
            C.List<String> traits = C.newList();
            String type = pair.right();
            if (type.contains("|")) {
                pair = S.binarySplit(type, '|');
                type = pair.left();
                traits.addAll(S.fastSplit(pair.right(), ","));
            }
            String prefix = S.cut(type).before("/");
            Trait trait = traitMap.get(prefix);
            if (null != trait) {
                traits.add(trait.name());
            }
            MimeType mimeType = new MimeType(fileExtension, type, traits.map(new $.Transformer<String, Trait>() {
                @Override
                public Trait transform(String s) {
                    return Trait.valueOf(s);
                }
            }));
            indexByFileExtension.put(fileExtension, mimeType);
            indexByContentType.put(type, mimeType);
        }
    }
}
