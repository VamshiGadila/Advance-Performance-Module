"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { getUser } from "@/lib/auth";

export default function Home() {
  const router = useRouter();
  useEffect(() => {
    const u = getUser();
    if (!u) router.replace("/login");
    else if (u.role === "HR") router.replace("/hr");
    else if (u.role === "MANAGER") router.replace("/manager");
    else router.replace("/employee");
  }, [router]);
  return <div className="content">Loading...</div>;
}