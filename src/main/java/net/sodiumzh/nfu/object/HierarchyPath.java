package net.sodiumzh.nfu.object;

import net.sodiumzh.nfu.annotation.NotYetImplemented;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@NotYetImplemented
public final class HierarchyPath {

    private final String[] splitPath;
    private static final HierarchyPath EMPTY = new HierarchyPath();

    /**
     * Directly copy the input array to this. Private to avoid external reference of the internal array.
     */
    private HierarchyPath(String... path) {
        this.splitPath = Arrays.copyOf(path, path.length);
    }

    public static HierarchyPath byNameArray(String... nameArray) {
        return new HierarchyPath(Arrays.copyOf(nameArray, nameArray.length));
    }

    public static HierarchyPath byLiteral(String literalPath) {
        return new HierarchyPath(splitLiteral(literalPath));
    }

    /**
     * Convert path to standard literal representation (like "/a/b/c/d/e").
     */
    public String toLiteral() {
        return formatLiteral(this.splitPath);
    }

    public static HierarchyPath empty() {
        return EMPTY;
    }

    @Nullable
    public HierarchyPath getParent() {
        if (splitPath.length == 0) return null;
        return new HierarchyPath(Arrays.copyOf(splitPath, splitPath.length - 1));
    }

    public int length() {
        return this.splitPath.length;
    }

    /**
     * Get the name at given position. For example: "/a/b/c/d/e" get at 0 => "a"; get at 2 => "c"; get at 5 (out of range) => throws.
     */
    public String getAt(int pos) {
        if (pos >= this.length())
            throw new IllegalArgumentException("Accessing position " + pos + "of path with length " + this.length());
        return this.splitPath[pos];
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof HierarchyPath cp)
            return Arrays.equals(this.splitPath, cp.splitPath);
        else return false;
    }

    /**
     * Test if this path is equal to the path represented by the input literal string.
     */
    public boolean equalsLiteral(String other) {
        return Arrays.equals(this.splitPath, splitLiteral(other));
    }

    /**
     * Check if this path represents an upstream node of the input path if from the same root. For example: "/a/b/cc" is upstream of "/a/b/cc/ddd" and "/a/b/cc/ddd/eeee".
     * <p>Note: False if input equals this.
     */
    public boolean isUpstreamOf(HierarchyPath other) {
        if (this.splitPath.length >= other.splitPath.length) return false;  // Upstream node's path length is always shorter
        for (int i = 0; i < this.splitPath.length; ++i) {
            if (!other.splitPath[i].equals(this.splitPath[i])) return false;
        }
        return true;
    }

    /**
     * Check if this path represents a downstream node of the input path if from the same root. For example: "/a/b/cc" is downstream of "/a" and "/a/b".
     * <p>Note: False if input equals this.
     */
    public boolean isDownstreamOf(HierarchyPath other) {
        return other.isUpstreamOf(this);
    }

    /**
     * Check if this path represents a direct parent node of the input path if from the same root. For example: "/a/b/cc" is direct parent of "/a/b/cc/ddd".
     */
    public boolean isDirectParentOf(HierarchyPath other) {
        return (this.splitPath.length + 1 == other.splitPath.length) && this.isUpstreamOf(other);
    }

    /**
     * Check if this path represents a direct child node of the input path if from the same root. For example: "/a/b/cc" is direct child of "/a/b".
     */
    public boolean isDirectChildOf(HierarchyPath other) {
        return other.isDirectParentOf(this);
    }

    /**
     * Get the path from the input path to this path. For example: if this is "/a/b/c/d/e" and input is "/a/b/c", then returns "/d/e".
     * @return Relative path from the input path to this path, or empty if this path is not downstream of the given path.
     */
    public Optional<HierarchyPath> absoluteToRelative(HierarchyPath relativeTo) {
        if (!relativeTo.isUpstreamOf(this)) return Optional.empty();
        return Optional.of(new HierarchyPath(Arrays.copyOfRange(this.splitPath, relativeTo.splitPath.length, this.splitPath.length)));
    }

    /**
     * Assume this path is a relative path from (a node with input absolute path), get the absolute path from root.
     * <p>For example: if this is "c/d/e" and input is "/a/b", then returns "/a/b/c/d/e".
     * @return Relative path from the input path to this path, or empty if this path is not downstream of the given path.
     */
    public HierarchyPath relativeToAbsolute(HierarchyPath relativeTo) {
        return new HierarchyPath(Stream.of(relativeTo.splitPath, this.splitPath).flatMap(Arrays::stream).toArray(String[]::new));
    }

    @Override
    public int hashCode() {
        return Objects.hash((Object[]) splitPath);
    }

    @Override
    public String toString() {
        return this.toLiteral();
    }

    public String[] toStringArray() {
        return Arrays.copyOf(this.splitPath, this.splitPath.length);
    }

    /** 
     * Split a literal to an array representation, cutting at '/' and '\'.
     * @param literal Any valid literal. A valid literal is several path strings connected with either '/' or '\'. e.g. "/a\\b/c///d" -> {a, b, c, d}.
     * @return Split from the input non-standard literal.
     */
    public static String[] splitLiteral(String literal) {
        return Arrays.stream(literal.split("[/\\\\]+")).filter(str -> !str.isEmpty()).toArray(String[]::new);
    }

    /**
     * Convert a string array representation to a standard literal (e.g. "/a/bb/ccc").
     */
    public static String formatLiteral(String[] split) {
        StringBuilder builder = new StringBuilder();
        Arrays.stream(split).forEach(str -> builder.append("/").append(str));
        return builder.toString();
    }

    /**
     * Format any valid literal to a standard literal. e.g. "/a\//bb\ccc//\d/" -> "/a/bb/ccc/d".
     */
    public static String formatLiteral(String rawLiteral) {
        return formatLiteral(splitLiteral(rawLiteral));
    }


}
