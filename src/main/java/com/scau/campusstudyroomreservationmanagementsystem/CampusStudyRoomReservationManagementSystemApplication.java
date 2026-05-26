package com.scau.campusstudyroomreservationmanagementsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CampusStudyRoomReservationManagementSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusStudyRoomReservationManagementSystemApplication.class, args);
    }

}
