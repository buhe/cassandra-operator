package com.instaclustr.cassandra.operator.k8s;

public class PatchOperation {
    private final String op = "replace";
    private final String path;
    private final Object value;

    public PatchOperation(String path, Object value) {
        this.path = path;
        this.value = value;
    }
}
