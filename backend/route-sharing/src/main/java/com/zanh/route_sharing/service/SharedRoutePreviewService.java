package com.zanh.route_sharing.service;

import com.zanh.route_sharing.dto.sharedroute.preview.PreviewSharedRouteRequest;
import com.zanh.route_sharing.dto.sharedroute.preview.SharedRoutePreviewResponse;

public interface SharedRoutePreviewService {

    SharedRoutePreviewResponse preview(
            Long actorUserId,
            Long sharedRouteId,
            PreviewSharedRouteRequest request);
}
