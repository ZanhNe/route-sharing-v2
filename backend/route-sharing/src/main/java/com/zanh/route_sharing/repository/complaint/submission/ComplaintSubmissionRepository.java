package com.zanh.route_sharing.repository.complaint.submission;

import com.zanh.route_sharing.repository.complaint.submission.model.ComplaintSubmissionCommand;
import com.zanh.route_sharing.repository.complaint.submission.model.ComplaintSubmissionResult;

public interface ComplaintSubmissionRepository {
    ComplaintSubmissionResult commit(ComplaintSubmissionCommand command);
}
