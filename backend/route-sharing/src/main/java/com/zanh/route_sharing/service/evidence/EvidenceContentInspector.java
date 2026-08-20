package com.zanh.route_sharing.service.evidence;

import java.nio.file.Path;

public interface EvidenceContentInspector {
    EvidenceInspection inspect(Path stagedPath);
}
