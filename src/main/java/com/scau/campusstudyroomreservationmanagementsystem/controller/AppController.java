package com.scau.campusstudyroomreservationmanagementsystem.controller;

import com.scau.campusstudyroomreservationmanagementsystem.service.AppService;
import com.scau.campusstudyroomreservationmanagementsystem.support.ApiResponse;
import com.scau.campusstudyroomreservationmanagementsystem.support.CurrentUser;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AppController {
    private final AppService app;

    public AppController(AppService app) {
        this.app = app;
    }

    @PostMapping("/auth/register")
    public ApiResponse<Map<String, Object>> register(@RequestBody Map<String, Object> req) {
        return ApiResponse.ok(app.register(req));
    }

    @PostMapping("/auth/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody Map<String, Object> req) {
        return ApiResponse.ok(app.loginStudent(req));
    }

    @PostMapping("/admin/auth/login")
    public ApiResponse<Map<String, Object>> adminLogin(@RequestBody Map<String, Object> req) {
        return ApiResponse.ok(app.loginAdmin(req));
    }

    @GetMapping("/auth/me")
    public ApiResponse<Map<String, Object>> me(@AuthenticationPrincipal CurrentUser user) {
        return ApiResponse.ok(user.isStudent() ? app.studentInfo(user) : app.adminInfo(user));
    }

    @PostMapping("/auth/change-password")
    public ApiResponse<Void> changePassword(@AuthenticationPrincipal CurrentUser user,
                                            @RequestBody Map<String, Object> req) {
        app.changePassword(user, req);
        return ApiResponse.ok(null);
    }

    @GetMapping("/student/profile")
    public ApiResponse<Map<String, Object>> profile(@AuthenticationPrincipal CurrentUser user) {
        return ApiResponse.ok(app.studentInfo(user));
    }

    @PutMapping("/student/profile")
    public ApiResponse<Map<String, Object>> updateProfile(@AuthenticationPrincipal CurrentUser user,
                                                           @RequestBody Map<String, Object> req) {
        return ApiResponse.ok(app.updateProfile(user, req));
    }

    @GetMapping("/rooms")
    public ApiResponse<List<Map<String, Object>>> rooms(@AuthenticationPrincipal CurrentUser user) {
        return ApiResponse.ok(app.rooms(user));
    }

    @GetMapping("/rooms/{id}")
    public ApiResponse<Map<String, Object>> room(@PathVariable Long id) {
        return ApiResponse.ok(app.room(id));
    }

    @GetMapping("/rooms/{id}/seats")
    public ApiResponse<List<Map<String, Object>>> roomSeats(@PathVariable Long id) {
        return ApiResponse.ok(app.seats(id));
    }

    @GetMapping("/seats/available")
    public ApiResponse<List<Map<String, Object>>> availableSeats(@RequestParam Long roomId,
                                                                  @RequestParam String date,
                                                                  @RequestParam String startTime,
                                                                  @RequestParam String endTime) {
        return ApiResponse.ok(app.availableSeats(roomId, date, startTime, endTime));
    }

    @PostMapping("/reservations")
    public ApiResponse<Map<String, Object>> createReservation(@AuthenticationPrincipal CurrentUser user,
                                                               @RequestBody Map<String, Object> req) {
        return ApiResponse.ok(app.createReservation(user, req));
    }

    @GetMapping("/reservations/my")
    public ApiResponse<List<Map<String, Object>>> myReservations(@AuthenticationPrincipal CurrentUser user,
                                                                  @RequestParam(required = false) String status,
                                                                  @RequestParam(required = false) Boolean today) {
        return ApiResponse.ok(app.myReservations(user, status, today));
    }

    @GetMapping("/reservations/{id}")
    public ApiResponse<Map<String, Object>> reservation(@PathVariable Long id) {
        return ApiResponse.ok(app.reservationDetail(id));
    }

    @PostMapping("/reservations/{id}/cancel")
    public ApiResponse<Void> cancel(@AuthenticationPrincipal CurrentUser user, @PathVariable Long id) {
        app.cancelReservation(user, id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/checkin/qrcode")
    public ApiResponse<Map<String, Object>> qr(@AuthenticationPrincipal CurrentUser user) {
        return ApiResponse.ok(app.qrCode(user));
    }

    @PostMapping("/reservations/{id}/checkout")
    public ApiResponse<Map<String, Object>> checkout(@AuthenticationPrincipal CurrentUser user, @PathVariable Long id) {
        return ApiResponse.ok(app.checkout(user, id));
    }

    @PostMapping("/reservations/{id}/temp-leave")
    public ApiResponse<Map<String, Object>> tempLeave(@AuthenticationPrincipal CurrentUser user, @PathVariable Long id) {
        return ApiResponse.ok(app.startTempLeave(user, id));
    }

    @PostMapping("/reservations/{id}/temp-return")
    public ApiResponse<Map<String, Object>> tempReturn(@AuthenticationPrincipal CurrentUser user, @PathVariable Long id) {
        return ApiResponse.ok(app.endTempLeave(user, id));
    }

    @GetMapping("/credits/my")
    public ApiResponse<Map<String, Object>> credit(@AuthenticationPrincipal CurrentUser user) {
        return ApiResponse.ok(app.credit(user));
    }

    @GetMapping("/statistics/my-study-duration")
    public ApiResponse<Map<String, Object>> myStudyDuration(@AuthenticationPrincipal CurrentUser user,
                                                             @RequestParam(required = false) String period) {
        return ApiResponse.ok(app.myStudyDuration(user, period));
    }

    @GetMapping("/announcements")
    public ApiResponse<List<Map<String, Object>>> announcements() {
        return ApiResponse.ok(app.announcements());
    }

    @PostMapping("/announcements/{id}/read")
    public ApiResponse<Void> readAnnouncement(@PathVariable Long id) {
        app.readAnnouncement(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/notifications")
    public ApiResponse<List<Map<String, Object>>> notifications(@AuthenticationPrincipal CurrentUser user) {
        return ApiResponse.ok(app.notifications(user));
    }

    @PostMapping("/notifications/{id}/read")
    public ApiResponse<Void> readNotification(@AuthenticationPrincipal CurrentUser user, @PathVariable Long id) {
        app.readNotification(user, id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/notifications/read-all")
    public ApiResponse<Void> readAllNotifications(@AuthenticationPrincipal CurrentUser user) {
        app.readAllNotifications(user);
        return ApiResponse.ok(null);
    }

    @PostMapping("/feedback")
    public ApiResponse<Map<String, Object>> feedback(@AuthenticationPrincipal CurrentUser user,
                                                      @RequestBody Map<String, Object> req) {
        return ApiResponse.ok(app.createFeedback(user, req));
    }

    @GetMapping("/feedback/my")
    public ApiResponse<List<Map<String, Object>>> myFeedback(@AuthenticationPrincipal CurrentUser user) {
        return ApiResponse.ok(app.myFeedback(user));
    }

    @GetMapping("/admin/dashboard")
    public ApiResponse<Map<String, Object>> dashboard(@AuthenticationPrincipal CurrentUser user) {
        return ApiResponse.ok(app.dashboard(user));
    }

    @GetMapping("/admin/users")
    public ApiResponse<List<Map<String, Object>>> users(@RequestParam(required = false) String keyword,
                                                         @RequestParam(required = false) String auditStatus) {
        return ApiResponse.ok(app.adminUsers(keyword, auditStatus));
    }

    @GetMapping("/admin/users/pending")
    public ApiResponse<List<Map<String, Object>>> pendingUsers() {
        return ApiResponse.ok(app.adminUsers(null, "PENDING"));
    }

    @PostMapping("/admin/users/{id}/approve")
    public ApiResponse<Void> approve(@AuthenticationPrincipal CurrentUser admin, @PathVariable Long id,
                                     @RequestBody(required = false) Map<String, Object> req) {
        app.auditUser(admin, id, true, req == null ? "" : String.valueOf(req.getOrDefault("remark", "")));
        return ApiResponse.ok(null);
    }

    @PostMapping("/admin/users/{id}/reject")
    public ApiResponse<Void> reject(@AuthenticationPrincipal CurrentUser admin, @PathVariable Long id,
                                    @RequestBody(required = false) Map<String, Object> req) {
        app.auditUser(admin, id, false, req == null ? "资料不符合要求" : String.valueOf(req.getOrDefault("remark", "资料不符合要求")));
        return ApiResponse.ok(null);
    }

    @PostMapping("/admin/users/{id}/disable")
    public ApiResponse<Void> disable(@PathVariable Long id) {
        app.setUserStatus(id, "DISABLED");
        return ApiResponse.ok(null);
    }

    @PostMapping("/admin/users/{id}/enable")
    public ApiResponse<Void> enable(@PathVariable Long id) {
        app.setUserStatus(id, "NORMAL");
        return ApiResponse.ok(null);
    }

    @GetMapping("/admin/rooms")
    public ApiResponse<List<Map<String, Object>>> adminRooms(@AuthenticationPrincipal CurrentUser user) {
        return ApiResponse.ok(app.rooms(user));
    }

    @PostMapping("/admin/rooms")
    public ApiResponse<Map<String, Object>> createRoom(@AuthenticationPrincipal CurrentUser user,
                                                        @RequestBody Map<String, Object> req) {
        return ApiResponse.ok(app.saveRoom(user, null, req));
    }

    @PutMapping("/admin/rooms/{id}")
    public ApiResponse<Map<String, Object>> updateRoom(@AuthenticationPrincipal CurrentUser user,
                                                        @PathVariable Long id,
                                                        @RequestBody Map<String, Object> req) {
        return ApiResponse.ok(app.saveRoom(user, id, req));
    }

    @DeleteMapping("/admin/rooms/{id}")
    public ApiResponse<Void> deleteRoom(@AuthenticationPrincipal CurrentUser user, @PathVariable Long id) {
        app.deleteRoom(user, id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/admin/rooms/{id}/seats")
    public ApiResponse<List<Map<String, Object>>> adminRoomSeats(@PathVariable Long id) {
        return ApiResponse.ok(app.seats(id));
    }

    @PutMapping("/admin/seats/{id}")
    public ApiResponse<Void> updateSeat(@PathVariable Long id, @RequestBody Map<String, Object> req) {
        app.updateSeat(id, req);
        return ApiResponse.ok(null);
    }

    @PutMapping("/admin/rooms/{id}/seats/batch")
    public ApiResponse<Void> batchSeats(@PathVariable Long id, @RequestBody Map<String, Object> req) {
        app.batchSeats(id, req);
        return ApiResponse.ok(null);
    }

    @PostMapping("/admin/rooms/{roomId}/seats")
    public ApiResponse<Map<String, Object>> createSeat(@AuthenticationPrincipal CurrentUser user,
                                                      @PathVariable Long roomId) {
        return ApiResponse.ok(app.addSeat(user, roomId));
    }

    @DeleteMapping("/admin/seats/{id}")
    public ApiResponse<Void> deleteSeat(@AuthenticationPrincipal CurrentUser user, @PathVariable Long id) {
        app.deleteSeat(user, id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/admin/reservations")
    public ApiResponse<List<Map<String, Object>>> adminReservations(@AuthenticationPrincipal CurrentUser user) {
        return ApiResponse.ok(app.adminReservations(user));
    }

    @PostMapping("/admin/checkin/scan")
    public ApiResponse<Map<String, Object>> scan(@AuthenticationPrincipal CurrentUser user,
                                                  @RequestBody Map<String, Object> req) {
        return ApiResponse.ok(app.scanCheckin(user, req));
    }

    @GetMapping("/admin/checkins")
    public ApiResponse<List<Map<String, Object>>> checkins(@AuthenticationPrincipal CurrentUser user) {
        return ApiResponse.ok(app.checkins(user));
    }

    @GetMapping("/admin/announcements")
    public ApiResponse<List<Map<String, Object>>> adminAnnouncements() {
        return ApiResponse.ok(app.announcements());
    }

    @PostMapping("/admin/announcements")
    public ApiResponse<Map<String, Object>> createAnnouncement(@AuthenticationPrincipal CurrentUser user,
                                                                @RequestBody Map<String, Object> req) {
        return ApiResponse.ok(app.saveAnnouncement(user, null, req));
    }

    @PutMapping("/admin/announcements/{id}")
    public ApiResponse<Map<String, Object>> updateAnnouncement(@AuthenticationPrincipal CurrentUser user,
                                                                @PathVariable Long id,
                                                                @RequestBody Map<String, Object> req) {
        return ApiResponse.ok(app.saveAnnouncement(user, id, req));
    }

    @DeleteMapping("/admin/announcements/{id}")
    public ApiResponse<Void> deleteAnnouncement(@PathVariable Long id) {
        app.deleteAnnouncement(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/admin/statistics/usage")
    public ApiResponse<List<Map<String, Object>>> usage(@AuthenticationPrincipal CurrentUser user,
                                                          @RequestParam(defaultValue = "day") String period) {
        return ApiResponse.ok(app.statisticsUsage(user, period));
    }

    @GetMapping("/admin/statistics/peak")
    public ApiResponse<List<Map<String, Object>>> peak(@AuthenticationPrincipal CurrentUser user,
                                                        @RequestParam(defaultValue = "day") String period) {
        return ApiResponse.ok(app.statisticsPeak(user, period));
    }

    @GetMapping("/admin/statistics/report")
    public ApiResponse<Map<String, Object>> statisticsReport(@AuthenticationPrincipal CurrentUser user,
                                                              @RequestParam(defaultValue = "day") String period) {
        return ApiResponse.ok(app.statisticsReport(user, period));
    }

    @GetMapping("/admin/statistics/credit")
    public ApiResponse<List<Map<String, Object>>> creditStats() {
        return ApiResponse.ok(app.statisticsCredit());
    }

    @GetMapping("/admin/statistics/export")
    public ResponseEntity<byte[]> export(@AuthenticationPrincipal CurrentUser user,
                                         @RequestParam(defaultValue = "day") String period) {
        byte[] bytes = ("\uFEFF" + app.exportCsv(user, period)).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=study-room-report.csv")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(bytes);
    }

    @GetMapping("/admin/feedback")
    public ApiResponse<List<Map<String, Object>>> adminFeedback(@AuthenticationPrincipal CurrentUser user) {
        return ApiResponse.ok(app.adminFeedback(user));
    }

    @PutMapping("/admin/feedback/{id}")
    public ApiResponse<Void> handleFeedback(@AuthenticationPrincipal CurrentUser user,
                                             @PathVariable Long id,
                                             @RequestBody Map<String, Object> req) {
        app.handleFeedback(user, id, req);
        return ApiResponse.ok(null);
    }

    @GetMapping("/admin/operation-logs")
    public ApiResponse<List<Map<String, Object>>> operationLogs(@AuthenticationPrincipal CurrentUser user) {
        return ApiResponse.ok(app.operationLogs(user));
    }

    @GetMapping("/admin/admins")
    public ApiResponse<List<Map<String, Object>>> adminAccounts(@AuthenticationPrincipal CurrentUser user) {
        return ApiResponse.ok(app.adminAccounts(user));
    }
}
