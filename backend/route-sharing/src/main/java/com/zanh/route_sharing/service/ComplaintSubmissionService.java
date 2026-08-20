package com.zanh.route_sharing.service;

import com.zanh.route_sharing.dto.complaint.submission.SubmitComplaintRequest;
import com.zanh.route_sharing.dto.complaint.submission.SubmitComplaintResponse;

public interface ComplaintSubmissionService {
    SubmitComplaintResponse submit(Long actorId, Long tripId, Long rideRequestId, SubmitComplaintRequest request);
}
