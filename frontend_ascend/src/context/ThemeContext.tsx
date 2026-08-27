"use client";

import React, { createContext, useContext, useEffect, useState } from "react";

type Theme = "dark" | "light";

interface ThemeContextType {
    theme: Theme;
    toggleTheme: () => void;
    setTheme: (theme: Theme) => void;
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined);

export function ThemeProvider({ children }: { children: React.ReactNode }) {
    const [theme, setThemeState] = useState<Theme>("dark");
    const [mounted, setMounted] = useState(false);

    useEffect(() => {
        setMounted(true);
        const saved = localStorage.getItem("ascend_theme") as Theme | null;
        if (saved === "dark" || saved === "light") {
            setThemeState(saved);
            document.documentElement.setAttribute("data-theme", saved);
        } else {
            // Check system preference or default to dark
            const prefersDark = window.matchMedia("(prefers-color-scheme: dark)").matches;
            const initial = prefersDark ? "dark" : "light";
            setThemeState(initial);
            document.documentElement.setAttribute("data-theme", initial);
        }
    }, []);

    const setTheme = (newTheme: Theme) => {
        setThemeState(newTheme);
        localStorage.setItem("ascend_theme", newTheme);
        document.documentElement.setAttribute("data-theme", newTheme);
    };

    const toggleTheme = () => {
        const nextTheme = theme === "dark" ? "light" : "dark";
        setTheme(nextTheme);
    };

    return (
        <ThemeContext.Provider value={{ theme, toggleTheme, setTheme }}>
            {children}
        </ThemeContext.Provider>
    );
}

export function useTheme() {
    const context = useContext(ThemeContext);
    if (!context) {
        throw new Error("useTheme must be used within a ThemeProvider");
    }
    return context;
}

export function ThemeToggle({ className = "" }: { className?: string }) {
    const { theme, toggleTheme } = useTheme();

    return (
        <button
            type="button"
            onClick={toggleTheme}
            className={`theme-toggle-btn ${className}`}
            title={`Switch to ${theme === "dark" ? "Light" : "Dark"} Mode`}
            aria-label="Toggle theme"
        >
            {theme === "dark" ? (
                <>
                    <span style={{ fontSize: "1rem" }}>☀️</span>
                    <span className="theme-toggle-label" style={{ fontSize: "0.8rem", fontWeight: "600" }}>Light</span>
                </>
            ) : (
                <>
                    <span style={{ fontSize: "1rem" }}>🌙</span>
                    <span className="theme-toggle-label" style={{ fontSize: "0.8rem", fontWeight: "600" }}>Dark</span>
                </>
            )}
        </button>
    );
}
