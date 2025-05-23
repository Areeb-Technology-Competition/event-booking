package com.example.demo.controllers;
import java.util.List;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import com.example.demo.models.Event;
import com.example.demo.models.User;
import com.example.demo.services.EventService;
import com.example.demo.services.UserService;

import jakarta.servlet.http.HttpSession;

@RestController
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private EventService eventService;

    @GetMapping("/signup")
    public ModelAndView addUser() {
        ModelAndView mav = new ModelAndView("/authentication/signup.html");
        User newUser = new User();
        mav.addObject("user", newUser);
        return mav;
    }
 
    @PostMapping("/signup")
    public ModelAndView saveUser(@Validated @ModelAttribute User user) {
        ModelAndView mav = new ModelAndView("/authentication/signup.html");
        mav.addObject("user", user);

        if (user.isEmpty(user.getFullname()) || user.isEmpty(user.getUsername()) ||
                user.isEmpty(user.getEmail()) || user.isEmpty(String.valueOf(user.getPhonenumber())) ||
                user.isEmpty(user.getPassword()) || user.isEmpty(user.getConfirmpassword())) {
            mav.addObject("emptyFieldsError", "Please fill in all fields");
            mav.addObject("hasEmptyFieldsError", true);
        } else {

            if (!user.isValidEmail(user.getEmail())) {
                mav.addObject("emailError", "Invalid email format");
                mav.addObject("hasEmailError", true);
            }
            if (isUsernameTaken(user.getUsername())) {
                mav.addObject("usernameTakenError", "Username is already taken");
                mav.addObject("hasUsernameTakenError", true);
            }

            if (!user.isPasswordValid(user.getPassword(), user.getConfirmpassword())) {
                if (user.getPassword().length() < 8) {
                    mav.addObject("passwordLengthError", "Password is too short (minimum 8 characters)");
                    mav.addObject("hasPasswordLengthError", true);
                }

                if (!user.getPassword().equals(user.getConfirmpassword())) {
                    mav.addObject("passwordMatchError", "Passwords do not match");
                    mav.addObject("hasPasswordMatchError", true);
                }

            }
            if (String.valueOf(user.getPhonenumber()).length() != 11) {
                mav.addObject("phoneNumberError", "Phone number must be 11 digits");
                mav.addObject("hasPhoneNumberError", true);
            }

            if (mav.getModel().containsKey("hasUsernameTakenError") ||
                    mav.getModel().containsKey("hasEmailError") ||
                    mav.getModel().containsKey("hasPasswordLengthError") ||
                    mav.getModel().containsKey("hasPasswordMatchError") ||
                    mav.getModel().containsKey("hasEmptyFieldsError") ||
                    mav.getModel().containsKey("hasPhoneNumberError"))
                return mav;

            String encodedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt(12));
            user.setPassword(encodedPassword);
            user.setRole("user");

            this.userService.saveUser(user);

            return new ModelAndView("redirect:/login");
        }
        return mav;
    }

    public boolean isUsernameTaken(String username) {
        User existingUser = userService.findByUsername(username);
        return existingUser != null;
    }

    @GetMapping("/login")
    public ModelAndView login() {
        ModelAndView mav = new ModelAndView("/authentication/login.html");
        mav.addObject("username");
        return mav;
    }
  
    @PostMapping("/login")
    public ModelAndView loginProcess(@RequestParam("username") String username,
            @RequestParam("password") String password, HttpSession session) {
        ModelAndView mav = new ModelAndView("/authentication/login.html");

        if (username == null || password == null) {
            mav.addObject("loginError", "Please provide both username and password");
            return mav;
        }

        User dbUser = userService.findByUsername(username);
        if (dbUser == null) {
            mav.addObject("loginError", "Username not found");
            mav.addObject("loginErrorField", "username");
            return mav;
        }

        boolean isPasswordMatched = BCrypt.checkpw(password, dbUser.getPassword());
        if (!isPasswordMatched) {
            mav.addObject("loginError", "Incorrect password");
            mav.addObject("loginErrorField", "password");
            return mav;
        }

        // Redirect to the index page after successful login
        session.setAttribute("user_id", dbUser.getId());
        session.setAttribute("username", dbUser.getUsername());
        session.setAttribute("role", dbUser.getRole());
        
        // Use service to determine dashboard based on role
        String dashboard = userService.getUserDashboard(dbUser.getId());
        return new ModelAndView("redirect:/" + dashboard);

    }
    @GetMapping("/logout")
    public ModelAndView logout(HttpSession session) {
        session.invalidate();
        return new ModelAndView("redirect:/user/index");
    }
    @GetMapping("/access-denied") 
    public ModelAndView accessDenied() {
        return new ModelAndView("/error/access_denied.html");
    }

    @GetMapping("/admin/dashboard")
    public ModelAndView adminIndex(HttpSession session) {  
        String role = (String)session.getAttribute("role");
        String name = (String)session.getAttribute("username");
        System.out.println(role);
        System.out.println(name);
        return new ModelAndView("admin/dashboard.html");
    }

    @GetMapping("/user/index")
    public ModelAndView userIndex(HttpSession session) {  
        String role = (String)session.getAttribute("role");
        String name = (String)session.getAttribute("username");
        System.out.println(role);
        System.out.println(name);
        ModelAndView mav = new ModelAndView("user/index.html");
        List<Event> events = this.eventService.findAllEvents();
        mav.addObject("events", events);
        return mav;

    }


}