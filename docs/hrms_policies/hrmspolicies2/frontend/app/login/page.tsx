"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";

interface LoginResponse {
  token?: string;
  message?: string;
  role?: string;
  email?: string;
}

const API_URL =
  process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

export default function LoginPage() {
  const router = useRouter();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function handleLogin() {
    setError("");

    if (!email.trim()) {
      setError("Please enter your email.");
      return;
    }

    if (!password) {
      setError("Please enter your password.");
      return;
    }

    setLoading(true);

    try {
      const response = await fetch(
        `${API_URL}/api/auth/login`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            email: email.trim(),
            password,
          }),
        }
      );

      const responseText = await response.text();

      let data: LoginResponse = {};

      if (responseText) {
        try {
          data = JSON.parse(responseText);
        } catch {
          throw new Error(
            "Backend returned invalid JSON."
          );
        }
      }

      if (!response.ok) {
        throw new Error(
          data.message ||
            "Invalid email or password."
        );
      }

      if (!data.token) {
        throw new Error(
          "JWT token was not returned by backend."
        );
      }

      localStorage.setItem(
        "token",
        data.token
      );

      localStorage.setItem(
        "email",
        data.email || email.trim()
      );

      localStorage.setItem(
        "role",
        data.role || "USER"
      );

      localStorage.setItem(
        "user",
        JSON.stringify({
          email:
            data.email || email.trim(),
          role:
            data.role || "USER",
        })
      );

      router.push("/policies");

    } catch (error) {
      if (error instanceof TypeError) {
        setError(
          "Unable to connect to Spring Boot. Make sure the backend is running on port 8080."
        );
      } else if (error instanceof Error) {
        setError(error.message);
      } else {
        setError(
          "Login failed."
        );
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-gray-100 px-4">

      <div className="w-full max-w-md rounded-2xl bg-white p-8 shadow-lg">

        <div className="mb-8 text-center">

          <h1 className="text-3xl font-bold text-gray-900">
            HRMS Policies 2
          </h1>

          <p className="mt-2 text-sm text-gray-500">
            Login to access Policies
          </p>

        </div>

        {error && (
          <div className="mb-5 rounded-lg bg-red-50 p-3 text-sm text-red-600">
            {error}
          </div>
        )}

        <div className="space-y-5">

          <div>

            <label className="mb-2 block text-sm font-medium">
              Email
            </label>

            <input
              type="email"
              value={email}
              placeholder="Enter your email"
              autoComplete="email"
              onChange={(event) =>
                setEmail(event.target.value)
              }
              className="w-full rounded-lg border border-gray-300 px-4 py-3 outline-none focus:border-blue-500"
            />

          </div>

          <div>

            <div className="mb-2 flex items-center justify-between">

              <label className="text-sm font-medium">
                Password
              </label>

              <Link
                href="/forgot-password"
                className="text-sm text-blue-600 hover:underline"
              >
                Forgot password?
              </Link>

            </div>

            <input
              type="password"
              value={password}
              placeholder="Enter your password"
              autoComplete="current-password"
              onChange={(event) =>
                setPassword(event.target.value)
              }
              onKeyDown={(event) => {
                if (event.key === "Enter") {
                  handleLogin();
                }
              }}
              className="w-full rounded-lg border border-gray-300 px-4 py-3 outline-none focus:border-blue-500"
            />

          </div>

          <button
            type="button"
            onClick={handleLogin}
            disabled={loading}
            className="w-full rounded-lg bg-blue-600 py-3 font-semibold text-white hover:bg-blue-700 disabled:bg-blue-400"
          >
            {loading
              ? "Signing in..."
              : "Login"}
          </button>

        </div>

        <div className="mt-6 text-center text-sm text-gray-600">

          Don't have an account?{" "}

          <Link
            href="/signup"
            className="font-semibold text-blue-600 hover:underline"
          >
            Create an account
          </Link>

        </div>

      </div>

    </main>
  );
}