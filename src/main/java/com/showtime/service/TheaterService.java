package com.showtime.service;

import com.showtime.dto.*;
import com.showtime.dto.response.*;
import com.showtime.dto.request.*;
import com.showtime.model.Theater;
import com.showtime.repository.TheaterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TheaterService {
    
    private final TheaterRepository theaterRepository;
    
    public List<TheaterDTO> getAllTheaters() {
        return theaterRepository.findAll().stream()
            .filter(Theater::isActive)
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    public List<String> getAllCities() {
        return theaterRepository.findAllCities();
    }
    
    public List<TheaterDTO> getTheatersByCity(String city) {
        return theaterRepository.findByCityIgnoreCaseAndIsActiveTrue(city).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    public TheaterDTO getTheaterById(Long id) {
        Theater theater = theaterRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Theater not found"));
        return convertToDTO(theater);
    }
    
    @Transactional
    public TheaterDTO createTheater(TheaterRequest request) {
        Theater theater = new Theater();
        updateTheaterFromRequest(theater, request);
        theater = theaterRepository.save(theater);
        return convertToDTO(theater);
    }
    
    @Transactional
    public TheaterDTO updateTheater(Long id, TheaterRequest request) {
        Theater theater = theaterRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Theater not found"));
        updateTheaterFromRequest(theater, request);
        theater = theaterRepository.save(theater);
        return convertToDTO(theater);
    }
    
    @Transactional
    public void deleteTheater(Long id) {
        Theater theater = theaterRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Theater not found"));
        theater.setActive(false);
        theaterRepository.save(theater);
    }
    
    public Long getTotalTheaters() {
        return theaterRepository.countActiveTheaters();
    }
    
    public Long getActiveTheatersCount() {
        return theaterRepository.countByIsActiveTrue();
    }
    
    private void updateTheaterFromRequest(Theater theater, TheaterRequest request) {
        theater.setName(request.getName());
        theater.setAddress(request.getAddress());
        theater.setCity(request.getCity());
        theater.setState(request.getState());
        theater.setPincode(request.getPincode());
        theater.setPhone(request.getPhone());
        theater.setEmail(request.getEmail());
        theater.setTotalScreens(request.getTotalScreens());
        theater.setFacilities(request.getFacilities());
    }
    
    private TheaterDTO convertToDTO(Theater theater) {
        TheaterDTO dto = new TheaterDTO();
        dto.setId(theater.getId());
        dto.setName(theater.getName());
        dto.setAddress(theater.getAddress());
        dto.setCity(theater.getCity());
        dto.setState(theater.getState());
        dto.setPincode(theater.getPincode());
        dto.setPhone(theater.getPhone());
        dto.setEmail(theater.getEmail());
        dto.setTotalScreens(theater.getTotalScreens());
        dto.setFacilities(theater.getFacilities());
        dto.setActive(theater.isActive());
        return dto;
    }
}