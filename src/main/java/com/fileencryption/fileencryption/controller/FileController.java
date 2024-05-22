package com.fileencryption.fileencryption.controller;

import com.fileencryption.fileencryption.service.FileEncryptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;

@Controller
public class FileController {

    @Autowired
    private FileEncryptionService fileEncryptionService;

    private Map<String, byte[]> encryptedFiles = new HashMap<>();
    private Map<String, SecretKey> fileKeys = new HashMap<>();
    private Map<String, byte[]> decryptedFiles = new HashMap<>();

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/upload")
    public String uploadFile(MultipartFile file, RedirectAttributes redirectAttributes, Model model) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "Please select a file to upload.");
            return "redirect:/";
        }

        try {
            SecretKey key = fileEncryptionService.generateKey();
            byte[] fileBytes = file.getBytes();
            byte[] encryptedFile = fileEncryptionService.encryptFile(fileBytes, key);
            String fileId = UUID.randomUUID().toString();
            encryptedFiles.put(fileId, encryptedFile);
            fileKeys.put(fileId, key);

            redirectAttributes.addFlashAttribute("message", "File uploaded and encrypted successfully.");
            redirectAttributes.addFlashAttribute("fileId", fileId);
        } catch (IOException | IllegalBlockSizeException | BadPaddingException e) {
            redirectAttributes.addFlashAttribute("message", "Failed to upload and encrypt file: " + e.getMessage());
            e.printStackTrace();
        }

        return "redirect:/";
    }

    @GetMapping("/download")
    @ResponseBody
    public byte[] downloadFile(@RequestParam("fileId") String fileId) {
        byte[] encryptedFile = encryptedFiles.get(fileId);
        if (encryptedFile == null) {
            throw new RuntimeException("File not found");
        }
        return encryptedFile;
    }

    @PostMapping("/decrypt")
    public String decryptFile(@RequestParam("fileId") String fileId, RedirectAttributes redirectAttributes, Model model) {
        byte[] encryptedFile = encryptedFiles.get(fileId);
        SecretKey key = fileKeys.get(fileId);
        if (encryptedFile == null || key == null) {
            redirectAttributes.addFlashAttribute("message", "File or key not found");
            return "redirect:/";
        }

        try {
            byte[] decryptedFile = fileEncryptionService.decryptFile(encryptedFile, key);
            decryptedFiles.put(fileId, decryptedFile);
            redirectAttributes.addFlashAttribute("message", "File decrypted successfully.");
            redirectAttributes.addFlashAttribute("fileId", fileId);
        } catch (IOException | IllegalBlockSizeException | BadPaddingException e) {
            redirectAttributes.addFlashAttribute("message", "Failed to decrypt file: " + e.getMessage());
            e.printStackTrace();
        }

        return "redirect:/";
    }

    @GetMapping("/downloadDecrypted")
    @ResponseBody
    public byte[] downloadDecryptedFile(@RequestParam("fileId") String fileId) {
        byte[] decryptedFile = decryptedFiles.get(fileId);
        if (decryptedFile == null) {
            throw new RuntimeException("File not found");
        }
        return decryptedFile;
    }
}

