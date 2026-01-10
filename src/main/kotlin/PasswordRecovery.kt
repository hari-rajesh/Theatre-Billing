package org.example

import org.example.data.Database
import org.example.data.UserDao
import java.io.File

/**
 * Utility script to recover or reset passwords for admin and user accounts.
 * 
 * Usage:
 * 1. To view all users and their passwords: Run this main function
 * 2. To reset passwords: Modify the resetPasswords() function and run it
 */
fun main() {
    println("=".repeat(60))
    println("Password Recovery Utility")
    println("=".repeat(60))
    println()
    
    // Display database location
    val dbPath = System.getProperty("user.home") + File.separator + "theatre_canteen.db"
    println("Database location: $dbPath")
    println("Database exists: ${File(dbPath).exists()}")
    println()
    
    try {
        // Initialize database connection
        val connection = Database.connection
        println("✓ Database connection established")
        println()
        
        // Get all users
        val users = UserDao.getAll()
        
        if (users.isEmpty()) {
            println("No users found in the database.")
            println("The default admin user should be created automatically on first run.")
            println("Default credentials: username='admin', password='password123'")
        } else {
            println("Found ${users.size} user(s):")
            println("-".repeat(60))
            
            users.forEach { user ->
                val role = if (user.isAdmin) "ADMIN" else "USER"
                println("ID: ${user.id}")
                println("Username: ${user.username}")
                println("Password: ${user.password}")
                println("Role: $role")
                println("-".repeat(60))
            }
        }
        
        println()
        println("=".repeat(60))
        println("To reset passwords, uncomment and modify the resetPasswords() function below")
        println("=".repeat(60))
        
        // Uncomment the following lines to reset passwords:
        // resetPasswords()
        
    } catch (e: Exception) {
        println("Error: ${e.message}")
        e.printStackTrace()
    }
}

/**
 * Function to reset passwords for admin and user accounts.
 * Uncomment the call to this function in main() to use it.
 */
fun resetPasswords() {
    println()
    println("Resetting passwords...")
    
    try {
        val users = UserDao.getAll()
        
        users.forEach { user ->
            val newPassword = if (user.isAdmin) {
                "admin123"  // New admin password
            } else {
                "user123"   // New user password
            }
            
            val updatedUser = user.copy(password = newPassword)
            val success = UserDao.update(updatedUser)
            
            if (success) {
                println("✓ Reset password for '${user.username}' (${if (user.isAdmin) "ADMIN" else "USER"}) to: $newPassword")
            } else {
                println("✗ Failed to reset password for '${user.username}'")
            }
        }
        
        println()
        println("Password reset complete!")
        
    } catch (e: Exception) {
        println("Error resetting passwords: ${e.message}")
        e.printStackTrace()
    }
}

