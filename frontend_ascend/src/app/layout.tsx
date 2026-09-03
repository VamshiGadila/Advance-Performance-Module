import "./globals.css";
import Shell from "@/components/Shell";
import { ThemeProvider } from "@/context/ThemeContext";
import type { Metadata } from "next";

export const metadata: Metadata = {
    title: "ASCEND - Performance Management System",
    description: "Performance & Goal Management Suite"
};

export default function RootLayout({
    children,
}: Readonly<{
    children: React.ReactNode;
}>) {
    return (
        <html lang="en">
            <body>
                <ThemeProvider>
                    <Shell>{children}</Shell>
                </ThemeProvider>
            </body>
        </html>
    );
}