package com.zanh.route_sharing.dto.complaint.evidence;
import java.util.List;
public record EvidencePageResponse(List<EvidenceItemResponse> items) { public EvidencePageResponse { items = List.copyOf(items); } }
