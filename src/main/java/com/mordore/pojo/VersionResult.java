package com.mordore.pojo;

import java.util.Map;

public record VersionResult (String latestAll, String latestRequired, Map<String, Map<String, ModVersion>> versionToMod) {}