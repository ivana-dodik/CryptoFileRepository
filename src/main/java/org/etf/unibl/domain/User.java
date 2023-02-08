package org.etf.unibl.domain;

public class User {

   private final int userId;
   private final String username;

   private boolean isActive;

   public User(int userId, String username, boolean isActive) {
      this.userId = userId;
      this.username = username;
      this.isActive = isActive;
   }

   public int getUserId() {
      return userId;
   }

   public String getUsername() {
      return username;
   }

   @Override
   public String toString() {
      return username;
   }

   public boolean isActive() {
      return isActive;
   }

   public void setActive(boolean active) {
      isActive = active;
   }
}
