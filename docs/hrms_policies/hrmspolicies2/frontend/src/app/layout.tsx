import "./globals.css";
import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "HRMS Policies Module",
  description: "Enterprise Policy Management & Compliance Portal",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
