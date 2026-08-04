package com.zanh.route_sharing.repository.sharedroute.preview;

import java.time.Instant;

import com.zanh.route_sharing.repository.sharedroute.preview.model.PreviewConsistencyToken;
import com.zanh.route_sharing.repository.sharedroute.preview.model.PreviewEvaluation;
import com.zanh.route_sharing.repository.sharedroute.preview.model.SharedRoutePreviewCriteria;

public interface SharedRoutePreviewRepository {

    PreviewEvaluation evaluate(SharedRoutePreviewCriteria criteria);

    boolean remainsCurrent(PreviewConsistencyToken token, Instant checkedAt);
}
