-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: May 31, 2025 at 11:38 AM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `event_system`
--

-- --------------------------------------------------------

--
-- Table structure for table `admin_accounts`
--

CREATE TABLE `admin_accounts` (
  `admin_id` varchar(50) NOT NULL,
  `password` varchar(100) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `admin_accounts`
--

INSERT INTO `admin_accounts` (`admin_id`, `password`) VALUES
('031204', 'adminpass');

-- --------------------------------------------------------

--
-- Table structure for table `courses`
--

CREATE TABLE `courses` (
  `id` int(11) NOT NULL,
  `course_name` varchar(100) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `courses`
--

INSERT INTO `courses` (`id`, `course_name`) VALUES
(9, 'BA in Political Science'),
(11, 'Bachelor in Industrial Technology'),
(10, 'BS in Accountancy'),
(5, 'BS in Agriculture'),
(1, 'BS in Civil Engineering'),
(2, 'BS in Electrical Engineering'),
(6, 'BS in Forestry'),
(7, 'BS in Hospitality Management'),
(4, 'BS in Information Technology'),
(3, 'BS in Mechanical Engineering'),
(8, 'BS in Tourism Management');

-- --------------------------------------------------------

--
-- Table structure for table `events`
--

CREATE TABLE `events` (
  `id` int(11) NOT NULL,
  `event_name` varchar(100) DEFAULT NULL,
  `date` date DEFAULT NULL,
  `course` varchar(50) DEFAULT NULL,
  `description` text DEFAULT NULL,
  `deadline` date DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `events`
--

INSERT INTO `events` (`id`, `event_name`, `date`, `course`, `description`, `deadline`) VALUES
(23, 'fwee', '2829-12-12', 'BS in Information Technology', 'fefefe', '2829-12-12'),
(26, 'fwefwefef', '2026-12-24', 'BS in Information Technology', 'ffwe', '2026-12-24'),
(28, 'Foundation Day', '2025-06-05', 'BS in Information Technology', 'happy me happy u', '2025-06-02'),
(29, 'kian', '2028-12-12', 'BS in Information Technology', 'es23dfrtgythj', '2028-12-12'),
(30, 'jenny', '2025-06-07', 'BS in Information Technology', 'gdrtgt', '2025-06-07'),
(31, 'Christmas party', '2025-06-13', 'BA in Political Science', 'gafsuqyusfgf', '2025-06-12');

-- --------------------------------------------------------

--
-- Table structure for table `event_requirements`
--

CREATE TABLE `event_requirements` (
  `id` int(11) NOT NULL,
  `event_id` int(11) DEFAULT NULL,
  `requirement_name` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `event_requirements`
--

INSERT INTO `event_requirements` (`id`, `event_id`, `requirement_name`) VALUES
(118, 23, 'wdwd'),
(119, 23, 'dwdwd'),
(121, 26, 'fweef'),
(127, 28, 'student id'),
(128, 28, 'waiver'),
(129, 29, 'cdcd'),
(130, 29, 'cdc'),
(132, 30, 'cdcd'),
(133, 30, 'swsw'),
(134, 31, 'id'),
(135, 31, 'greenform');

-- --------------------------------------------------------

--
-- Table structure for table `password_resets`
--

CREATE TABLE `password_resets` (
  `id` int(11) NOT NULL,
  `student_id` varchar(20) DEFAULT NULL,
  `new_password` varchar(100) DEFAULT NULL,
  `reason` text DEFAULT NULL,
  `status` varchar(20) DEFAULT 'pending'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `password_resets`
--

INSERT INTO `password_resets` (`id`, `student_id`, `new_password`, `reason`, `status`) VALUES
(1, '25353453', '123', '12313', 'rejected'),
(18, '312', '123', 'cxff', 'pending');

-- --------------------------------------------------------

--
-- Table structure for table `submissions`
--

CREATE TABLE `submissions` (
  `id` int(11) NOT NULL,
  `student_id` varchar(50) DEFAULT NULL,
  `event_id` int(11) DEFAULT NULL,
  `requirement_text` text DEFAULT NULL,
  `status` enum('pending','approved','rejected') DEFAULT 'pending',
  `admin_notes` text DEFAULT NULL,
  `file_path` varchar(255) DEFAULT NULL,
  `timestamp` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `submissions`
--

INSERT INTO `submissions` (`id`, `student_id`, `event_id`, `requirement_text`, `status`, `admin_notes`, `file_path`, `timestamp`) VALUES
(29, '312', 17, '32323', 'pending', NULL, 'C:\\Users\\bihas\\Downloads\\kian22.jpg', '2025-05-27 16:28:14'),
(30, '312', 17, 'school id', 'pending', NULL, 'C:\\Users\\bihas\\Downloads\\TECH101-Proposal.pdf', '2025-05-27 16:28:14'),
(31, '312', 17, 'ewqew', 'pending', NULL, 'C:\\Users\\bihas\\Downloads\\resume.png', '2025-05-27 16:28:14'),
(32, '312', 17, 'self', 'pending', NULL, 'C:\\Users\\bihas\\Downloads\\menu(1).png', '2025-05-27 16:28:14'),
(33, '312', 17, 'gurdian id', 'pending', NULL, 'C:\\Users\\bihas\\Downloads\\TECH101-Proposal.pdf', '2025-05-27 16:28:14'),
(34, '312', 7, 'e3e3e', 'pending', NULL, 'C:\\Users\\bihas\\Downloads\\CamScanner 12-04-2024 13.55.docx', '2025-05-27 21:42:15'),
(35, '312', 26, 'fweef', 'approved', NULL, 'C:\\Users\\bihas\\Downloads\\LIST OF MEMBERS OF GROUP 8 CLASS 012-HRMSC 2024.docx', '2025-05-27 23:18:35'),
(36, '312', 27, 'dsd', 'pending', NULL, 'C:\\Users\\bihas\\Downloads\\INNOVENTS.docx', '2025-05-27 23:47:37'),
(37, '312', 23, 'dwdwd', 'approved', NULL, 'C:\\Users\\bihas\\Downloads\\image_50729985.JPG', '2025-05-27 23:53:45'),
(38, '312', 23, 'wdwd', 'approved', NULL, 'C:\\Users\\bihas\\Downloads\\jenny.jpg', '2025-05-27 23:53:45'),
(39, '312', 21, 'ffrfr', 'approved', NULL, 'C:\\Users\\bihas\\Downloads\\menu(2).png', '2025-05-28 03:00:52'),
(42, '0428', 28, 'student id', 'approved', NULL, 'C:\\Users\\bihas\\Downloads\\qwerty.png', '2025-05-29 01:12:35'),
(43, '0428', 28, 'waiver', 'approved', NULL, 'C:\\Users\\bihas\\Downloads\\menu.png', '2025-05-29 01:12:35'),
(44, '312', 29, 'cdc', 'approved', NULL, 'C:\\Users\\bihas\\Downloads\\submission.png', '2025-05-29 01:22:31'),
(45, '312', 29, 'ccdcd', 'approved', NULL, 'C:\\Users\\bihas\\Downloads\\right-arrow(1).png', '2025-05-29 01:22:31'),
(46, '312', 29, 'cdcd', 'approved', NULL, 'C:\\Users\\bihas\\Downloads\\rotation-lock(1).png', '2025-05-29 01:22:31'),
(49, '312', 28, 'student id', 'approved', NULL, 'C:\\Users\\bihas\\Downloads\\resume.png', '2025-05-29 01:31:08'),
(50, '312', 28, 'waiver', 'approved', NULL, 'C:\\Users\\bihas\\Downloads\\plus.png', '2025-05-29 01:31:08'),
(51, '312', 30, 'cdcd', 'approved', NULL, 'C:\\Users\\bihas\\Downloads\\kianpogi1.jpg', '2025-05-29 01:54:45'),
(52, '312', 30, 'swsw', 'approved', NULL, 'C:\\Users\\bihas\\Downloads\\trash.png', '2025-05-29 01:54:45'),
(53, '0427', 31, 'id', 'approved', NULL, 'C:\\Users\\bihas\\Downloads\\BIHASA (3).docx', '2025-05-29 13:33:51'),
(54, '0427', 31, 'greenform', 'approved', NULL, 'C:\\Users\\bihas\\Downloads\\arrow.png', '2025-05-29 13:33:51'),
(55, '123', 31, 'id', 'approved', NULL, 'C:\\Users\\bihas\\Downloads\\kian22.jpg', '2025-05-29 16:34:11'),
(56, '123', 31, 'greenform', 'approved', NULL, 'C:\\Users\\bihas\\Downloads\\image_6483441 (1).JPG', '2025-05-29 16:34:11');

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `id` int(11) NOT NULL,
  `student_id` varchar(20) NOT NULL,
  `full_name` varchar(100) NOT NULL,
  `password` varchar(255) NOT NULL,
  `course` varchar(50) NOT NULL,
  `status` enum('pending','approved','rejected') DEFAULT 'pending',
  `rejection_note` varchar(255) DEFAULT NULL,
  `first_login_notice` tinyint(1) DEFAULT 1,
  `is_enabled` tinyint(1) DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`id`, `student_id`, `full_name`, `password`, `course`, `status`, `rejection_note`, `first_login_notice`, `is_enabled`) VALUES
(16, '312', 'kian ernest bihasa', '312', 'BS in Information Technology', 'approved', NULL, 0, 1),
(20, '0428', 'jenny pogi', '123', 'BS in Accountancy', 'approved', NULL, 0, 1),
(21, '1234', 'ded tgrtgt', '1234', 'BA in Political Science', 'approved', NULL, 0, 1);

--
-- Indexes for dumped tables
--

--
-- Indexes for table `admin_accounts`
--
ALTER TABLE `admin_accounts`
  ADD PRIMARY KEY (`admin_id`);

--
-- Indexes for table `courses`
--
ALTER TABLE `courses`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `course_name` (`course_name`);

--
-- Indexes for table `events`
--
ALTER TABLE `events`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `event_requirements`
--
ALTER TABLE `event_requirements`
  ADD PRIMARY KEY (`id`),
  ADD KEY `event_id` (`event_id`);

--
-- Indexes for table `password_resets`
--
ALTER TABLE `password_resets`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `submissions`
--
ALTER TABLE `submissions`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `courses`
--
ALTER TABLE `courses`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=14;

--
-- AUTO_INCREMENT for table `events`
--
ALTER TABLE `events`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=32;

--
-- AUTO_INCREMENT for table `event_requirements`
--
ALTER TABLE `event_requirements`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=136;

--
-- AUTO_INCREMENT for table `password_resets`
--
ALTER TABLE `password_resets`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=19;

--
-- AUTO_INCREMENT for table `submissions`
--
ALTER TABLE `submissions`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=57;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=24;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `event_requirements`
--
ALTER TABLE `event_requirements`
  ADD CONSTRAINT `event_requirements_ibfk_1` FOREIGN KEY (`event_id`) REFERENCES `events` (`id`) ON DELETE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
