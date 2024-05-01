package ru.infinitesynergy.bankapi.dto;

public record TransferDto(String recipientLogin, double amount) {
}
