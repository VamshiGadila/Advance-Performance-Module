"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";

const API_URL =
  process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

export default function SignupPage() {

  const router = useRouter();

  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] =
    useState("");

  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [loading, setLoading] = useState(false);

  async function handleSignup() {

    setError("");
    setSuccess("");

    if (
      !name.trim() ||
      !email.trim() ||
      !password ||
      !confirmPassword
    ) {
      setError(
        "Please fill all fields."
      );
      return;
    }

    if (password.length < 6) {
      setError(
        "Password must be at least 6 characters."
      );
      return;
    }

    if (password !== confirmPassword) {
      setError(
        "Passwords do not match."
      );
      return;
    }

    setLoading(true);

    try {

      const response = await fetch(
        `${API_URL}/api/auth/signup`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            name: name.trim(),
            email: email.trim(),
            password,
          }),
        }
      );

      const text =
        await response.text();

      let data: {
        message?: string;
      } = {};

      if (text) {
        try {
          data = JSON.parse(text);
        } catch {}
      }

      if (!response.ok) {
        throw new Error(
          data.message ||
            "Signup failed."
        );
      }

      setSuccess(
        "Account created successfully."
      );

      setTimeout(() => {
        router.push("/login");
      }, 1000);

    } catch (error) {

      setError(
        error instanceof Error
          ? error.message
          : "Signup failed."
      );

    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-gray-100 px-4">

      <div className="w-full max-w-md rounded-2xl bg-white p-8 shadow-lg">

        <h1 className="text-center text-3xl font-bold">
          Create Account
        </h1>

        {error && (
          <div className="mt-5 rounded-lg bg-red-50 p-3 text-sm text-red-600">
            {error}
          </div>
        )}

        {success && (
          <div className="mt-5 rounded-lg bg-green-50 p-3 text-sm text-green-600">
            {success}
          </div>
        )}

        <div className="mt-6 space-y-4">

          <input
            placeholder="Full name"
            value={name}
            onChange={(e) =>
              setName(e.target.value)
            }
            className="w-full rounded-lg border px-4 py-3"
          />

          <input
            type="email"
            placeholder="Email"
            value={email}
            onChange={(e) =>
              setEmail(e.target.value)
            }
            className="w-full rounded-lg border px-4 py-3"
          />

          <input
            type="password"
            placeholder="Password"
            value={password}
            onChange={(e) =>
              setPassword(e.target.value)
            }
            className="w-full rounded-lg border px-4 py-3"
          />

          <input
            type="password"
            placeholder="Confirm password"
            value={confirmPassword}
            onChange={(e) =>
              setConfirmPassword(e.target.value)
            }
            className="w-full rounded-lg border px-4 py-3"
          />

          <button
            onClick={handleSignup}
            disabled={loading}
            className="w-full rounded-lg bg-blue-600 py-3 font-semibold text-white disabled:bg-blue-400"
          >
            {loading
              ? "Creating..."
              : "Create Account"}
          </button>

        </div>

        <p className="mt-6 text-center text-sm">

          Already have an account?{" "}

          <Link
            href="/login"
            className="font-semibold text-blue-600"
          >
            Login
          </Link>

        </p>

      </div>

    </main>
  );
}