"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";

interface Policy {
  id: number;
  name: string;
  code: string;
  category: string;
  content: string;
  applicability: string;
  mandatory: boolean;
  status: string;
}

interface PolicyForm {
  name: string;
  code: string;
  category: string;
  content: string;
  applicability: string;
  mandatory: boolean;
  status: string;
}

const API_URL =
  process.env.NEXT_PUBLIC_API_URL ||
  "http://localhost:8080";

const emptyForm: PolicyForm = {
  name: "",
  code: "",
  category: "",
  content: "",
  applicability: "ALL",
  mandatory: false,
  status: "DRAFT",
};

export default function PoliciesPage() {
  const router = useRouter();

  // ==========================================
  // STATE
  // ==========================================

  const [policies, setPolicies] =
    useState<Policy[]>([]);

  const [form, setForm] =
    useState<PolicyForm>(emptyForm);

  const [editingId, setEditingId] =
    useState<number | null>(null);

  const [patchMode, setPatchMode] =
    useState(false);

  const [originalPolicy, setOriginalPolicy] =
    useState<Policy | null>(null);

  const [error, setError] =
    useState("");

  const [message, setMessage] =
    useState("");

  const [loading, setLoading] =
    useState(true);

  const [saving, setSaving] =
    useState(false);

  // ==========================================
  // CHECK LOGIN
  // ==========================================

  useEffect(() => {
    const token =
      localStorage.getItem("token");

    if (!token) {
      router.replace("/login");
      return;
    }

    loadPolicies();
  }, [router]);

  // ==========================================
  // API REQUEST
  // ==========================================

  async function apiRequest(
    path: string,
    options: RequestInit = {}
  ) {
    const token =
      localStorage.getItem("token");

    const response = await fetch(
      `${API_URL}${path}`,
      {
        ...options,

        headers: {
          "Content-Type":
            "application/json",

          Authorization:
            `Bearer ${token}`,

          ...(options.headers || {}),
        },
      }
    );

    // ========================================
    // UNAUTHORIZED
    // ========================================

    if (response.status === 401) {
      localStorage.clear();

      router.replace("/login");

      throw new Error(
        "Session expired. Please login again."
      );
    }

    const text =
      await response.text();

    let data: unknown = null;

    if (text) {
      try {
        data = JSON.parse(text);
      } catch {
        data = text;
      }
    }

    if (!response.ok) {
      const errorMessage =
        typeof data === "object" &&
        data !== null &&
        "message" in data
          ? String(
              (
                data as {
                  message: unknown;
                }
              ).message
            )
          : typeof data === "string"
          ? data
          : "Request failed";

      throw new Error(errorMessage);
    }

    return data;
  }

  // ==========================================
  // GET ALL POLICIES
  // ==========================================

  async function loadPolicies() {
    try {
      setLoading(true);
      setError("");

      const response =
        await apiRequest(
          "/api/policies"
        );

      // Backend now wraps successful responses as
      // { success, message, data, timestamp }.
      const list =
        response &&
        typeof response === "object" &&
        "data" in response
          ? (response as { data: unknown }).data
          : response;

      setPolicies(
        Array.isArray(list)
          ? (list as Policy[])
          : []
      );
    } catch (error) {
      setError(
        error instanceof Error
          ? error.message
          : "Unable to load policies."
      );
    } finally {
      setLoading(false);
    }
  }

  // ==========================================
  // UPDATE FORM FIELD
  // ==========================================

  function updateField(
    field: keyof PolicyForm,
    value: string | boolean
  ) {
    setForm((current) => ({
      ...current,
      [field]: value,
    }));
  }

  // ==========================================
  // CREATE / PUT / PATCH
  // ==========================================

  async function handleSubmit(
    event: React.FormEvent<HTMLFormElement>
  ) {
    event.preventDefault();

    setError("");
    setMessage("");
    setSaving(true);

    try {
      // ======================================
      // PATCH
      // ======================================

      if (
        patchMode &&
        editingId !== null
      ) {
        if (!originalPolicy) {
          setError(
            "Original policy data is missing."
          );

          return;
        }

        const patchData: Partial<PolicyForm> =
          {};

        // ------------------------------------
        // NAME
        // ------------------------------------

        if (
          form.name !==
          originalPolicy.name
        ) {
          patchData.name =
            form.name;
        }

        // ------------------------------------
        // CODE
        // ------------------------------------

        if (
          form.code !==
          originalPolicy.code
        ) {
          patchData.code =
            form.code;
        }

        // ------------------------------------
        // CATEGORY
        // ------------------------------------

        if (
          form.category !==
          originalPolicy.category
        ) {
          patchData.category =
            form.category;
        }

        // ------------------------------------
        // CONTENT
        // ------------------------------------

        if (
          form.content !==
          originalPolicy.content
        ) {
          patchData.content =
            form.content;
        }

        // ------------------------------------
        // APPLICABILITY
        // ------------------------------------

        if (
          form.applicability !==
          originalPolicy.applicability
        ) {
          patchData.applicability =
            form.applicability;
        }

        // ------------------------------------
        // MANDATORY
        // ------------------------------------

        if (
          form.mandatory !==
          originalPolicy.mandatory
        ) {
          patchData.mandatory =
            form.mandatory;
        }

        // ------------------------------------
        // STATUS
        // ------------------------------------

        if (
          form.status !==
          originalPolicy.status
        ) {
          patchData.status =
            form.status;
        }

        // ------------------------------------
        // NO CHANGES
        // ------------------------------------

        if (
          Object.keys(patchData)
            .length === 0
        ) {
          setMessage(
            "No changes were made."
          );

          return;
        }

        // ------------------------------------
        // SEND PATCH
        // ------------------------------------

        await apiRequest(
          `/api/policies/${editingId}`,
          {
            method: "PATCH",

            body: JSON.stringify(
              patchData
            ),
          }
        );

        setMessage(
          "Policy updated successfully using PATCH."
        );

        setEditingId(null);
        setPatchMode(false);
        setOriginalPolicy(null);
        setForm(emptyForm);

        await loadPolicies();

        return;
      }

      // ======================================
      // PUT
      // ======================================

      if (
        editingId !== null
      ) {
        await apiRequest(
          `/api/policies/${editingId}`,
          {
            method: "PUT",

            body: JSON.stringify(
              form
            ),
          }
        );

        setMessage(
          "Policy updated successfully."
        );

        setEditingId(null);
        setPatchMode(false);
        setOriginalPolicy(null);
        setForm(emptyForm);

        await loadPolicies();

        return;
      }

      // ======================================
      // POST
      // ======================================

      await apiRequest(
        "/api/policies",
        {
          method: "POST",

          body: JSON.stringify(
            form
          ),
        }
      );

      setMessage(
        "Policy created successfully."
      );

      setForm(emptyForm);

      await loadPolicies();

    } catch (error) {
      setError(
        error instanceof Error
          ? error.message
          : "Operation failed."
      );
    } finally {
      setSaving(false);
    }
  }

  // ==========================================
  // PATCH BUTTON
  // ==========================================

  function handlePatch(
    policy: Policy
  ) {
    setError("");
    setMessage("");

    setEditingId(policy.id);

    setPatchMode(true);

    setOriginalPolicy({
      ...policy,
    });

    setForm({
      name: policy.name,
      code: policy.code,
      category: policy.category,
      content: policy.content,
      applicability:
        policy.applicability,
      mandatory:
        policy.mandatory,
      status:
        policy.status,
    });

    window.scrollTo({
      top: 0,
      behavior: "smooth",
    });
  }

  // ==========================================
  // PUT / EDIT BUTTON
  // ==========================================

  function editPolicy(
    policy: Policy
  ) {
    setError("");
    setMessage("");

    setEditingId(policy.id);

    setPatchMode(false);

    setOriginalPolicy(null);

    setForm({
      name: policy.name,
      code: policy.code,
      category: policy.category,
      content: policy.content,
      applicability:
        policy.applicability,
      mandatory:
        policy.mandatory,
      status:
        policy.status,
    });

    window.scrollTo({
      top: 0,
      behavior: "smooth",
    });
  }

  // ==========================================
  // DELETE
  // ==========================================

  async function handleDelete(
    id: number
  ) {
    const confirmed =
      window.confirm(
        "Are you sure you want to delete this policy?"
      );

    if (!confirmed) {
      return;
    }

    try {
      setError("");
      setMessage("");

      await apiRequest(
        `/api/policies/${id}`,
        {
          method: "DELETE",
        }
      );

      setMessage(
        "Policy deleted successfully."
      );

      await loadPolicies();

    } catch (error) {
      setError(
        error instanceof Error
          ? error.message
          : "Delete failed."
      );
    }
  }

  // ==========================================
  // CANCEL EDIT / PATCH
  // ==========================================

  function cancelEdit() {
    setEditingId(null);

    setPatchMode(false);

    setOriginalPolicy(null);

    setForm(emptyForm);

    setError("");
    setMessage("");
  }

  // ==========================================
  // LOGOUT
  // ==========================================

  function logout() {
    localStorage.clear();

    router.replace("/login");
  }

  // ==========================================
  // UI
  // ==========================================

  return (
    <main className="min-h-screen bg-gray-100 p-6">

      <div className="mx-auto max-w-7xl">

        {/* =====================================
            HEADER
        ====================================== */}

        <div className="mb-6 flex items-center justify-between">

          <div>
            <h1 className="text-3xl font-bold text-gray-900">
              HRMS Policies 2
            </h1>

            <p className="text-gray-500">
              Policy Management
            </p>
          </div>

          <button
            type="button"
            onClick={logout}
            className="rounded-lg bg-red-600 px-5 py-2 font-semibold text-white hover:bg-red-700"
          >
            Logout
          </button>

        </div>

        {/* =====================================
            ERROR
        ====================================== */}

        {error && (
          <div className="mb-4 rounded-lg bg-red-50 p-3 text-red-600">
            {error}
          </div>
        )}

        {/* =====================================
            SUCCESS
        ====================================== */}

        {message && (
          <div className="mb-4 rounded-lg bg-green-50 p-3 text-green-600">
            {message}
          </div>
        )}

        {/* =====================================
            FORM
        ====================================== */}

        <form
          onSubmit={handleSubmit}
          className="mb-8 rounded-2xl bg-white p-6 shadow"
        >

          <div className="mb-5 flex items-center justify-between">

            <div>

              <h2 className="text-xl font-bold">

                {editingId === null
                  ? "Create Policy"
                  : patchMode
                  ? "Patch Policy"
                  : "Edit Policy"}

              </h2>

              {patchMode && (
                <p className="mt-1 text-sm text-purple-600">
                  Change any field you want and click PATCH.
                </p>
              )}

            </div>

          </div>

          {/* ===================================
              FORM FIELDS
          ==================================== */}

          <div className="grid gap-4 md:grid-cols-2">

            {/* NAME */}

            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700">
                Policy Name
              </label>

              <input
                value={form.name}
                onChange={(e) =>
                  updateField(
                    "name",
                    e.target.value
                  )
                }
                placeholder="Policy name"
                className="w-full rounded-lg border px-4 py-3"
              />
            </div>

            {/* CODE */}

            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700">
                Policy Code
              </label>

              <input
                value={form.code}
                onChange={(e) =>
                  updateField(
                    "code",
                    e.target.value
                  )
                }
                placeholder="Policy code"
                className="w-full rounded-lg border px-4 py-3"
              />
            </div>

            {/* CATEGORY */}

            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700">
                Category
              </label>

              <input
                value={form.category}
                onChange={(e) =>
                  updateField(
                    "category",
                    e.target.value
                  )
                }
                placeholder="Category"
                className="w-full rounded-lg border px-4 py-3"
              />
            </div>

            {/* APPLICABILITY */}

            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700">
                Applicability
              </label>

              <select
                value={
                  form.applicability
                }
                onChange={(e) =>
                  updateField(
                    "applicability",
                    e.target.value
                  )
                }
                className="w-full rounded-lg border px-4 py-3"
              >
                <option value="ALL">
                  ALL
                </option>

                <option value="GRADE_BASED">
                  GRADE_BASED
                </option>

                <option value="DEPT_BASED">
                  DEPT_BASED
                </option>
              </select>
            </div>

            {/* CONTENT */}

            <div className="md:col-span-2">

              <label className="mb-1 block text-sm font-medium text-gray-700">
                Content
              </label>

              <textarea
                value={form.content}
                onChange={(e) =>
                  updateField(
                    "content",
                    e.target.value
                  )
                }
                placeholder="Policy content"
                className="min-h-32 w-full rounded-lg border px-4 py-3"
              />

            </div>

            {/* STATUS */}

            <div>

              <label className="mb-1 block text-sm font-medium text-gray-700">
                Status
              </label>

              <select
                value={form.status}
                onChange={(e) =>
                  updateField(
                    "status",
                    e.target.value
                  )
                }
                className="w-full rounded-lg border px-4 py-3"
              >

                <option value="DRAFT">
                  DRAFT
                </option>

                <option value="APPROVED">
                  APPROVED
                </option>

                <option value="PUBLISHED">
                  PUBLISHED
                </option>

                <option value="ARCHIVED">
                  ARCHIVED
                </option>

              </select>

            </div>

            {/* MANDATORY */}

            <div className="flex items-center">

              <label className="flex items-center gap-3">

                <input
                  type="checkbox"
                  checked={
                    form.mandatory
                  }
                  onChange={(e) =>
                    updateField(
                      "mandatory",
                      e.target.checked
                    )
                  }
                  className="h-4 w-4"
                />

                <span className="font-medium">
                  Mandatory
                </span>

              </label>

            </div>

          </div>

          {/* ===================================
              BUTTONS
          ==================================== */}

          <div className="mt-6 flex flex-wrap gap-3">

            {/* CREATE */}

            {editingId === null && (
              <button
                type="submit"
                disabled={saving}
                className="rounded-lg bg-green-600 px-5 py-3 font-semibold text-white hover:bg-green-700 disabled:opacity-50"
              >
                {saving
                  ? "Creating..."
                  : "Create Policy"}
              </button>
            )}

            {/* PUT */}

            {editingId !== null &&
              !patchMode && (
                <button
                  type="submit"
                  disabled={saving}
                  className="rounded-lg bg-blue-600 px-5 py-3 font-semibold text-white hover:bg-blue-700 disabled:opacity-50"
                >
                  {saving
                    ? "Updating..."
                    : "Update Policy"}
                </button>
              )}

            {/* PATCH */}

            {editingId !== null &&
              patchMode && (
                <button
                  type="submit"
                  disabled={saving}
                  className="rounded-lg bg-purple-600 px-5 py-3 font-semibold text-white hover:bg-purple-700 disabled:opacity-50"
                >
                  {saving
                    ? "Patching..."
                    : "Apply PATCH"}
                </button>
              )}

            {/* CANCEL */}

            {editingId !== null && (
              <button
                type="button"
                onClick={cancelEdit}
                className="rounded-lg bg-gray-500 px-5 py-3 font-semibold text-white hover:bg-gray-600"
              >
                Cancel
              </button>
            )}

          </div>

        </form>

        {/* =====================================
            POLICY TABLE
        ====================================== */}

        <div className="rounded-2xl bg-white p-6 shadow">

          <h2 className="mb-5 text-xl font-bold">
            Policies
          </h2>

          {loading ? (

            <p>
              Loading...
            </p>

          ) : policies.length === 0 ? (

            <p className="text-gray-500">
              No policies found.
            </p>

          ) : (

            <div className="overflow-x-auto">

              <table className="w-full">

                <thead>

                  <tr className="border-b text-left">

                    <th className="p-3">
                      Name
                    </th>

                    <th className="p-3">
                      Code
                    </th>

                    <th className="p-3">
                      Category
                    </th>

                    <th className="p-3">
                      Applicability
                    </th>

                    <th className="p-3">
                      Status
                    </th>

                    <th className="p-3">
                      Mandatory
                    </th>

                    <th className="p-3">
                      Actions
                    </th>

                  </tr>

                </thead>

                <tbody>

                  {policies.map(
                    (policy) => (

                      <tr
                        key={policy.id}
                        className="border-b"
                      >

                        <td className="p-3">
                          {policy.name}
                        </td>

                        <td className="p-3">
                          {policy.code}
                        </td>

                        <td className="p-3">
                          {policy.category}
                        </td>

                        <td className="p-3">
                          {policy.applicability}
                        </td>

                        <td className="p-3">
                          {policy.status}
                        </td>

                        <td className="p-3">
                          {policy.mandatory
                            ? "Yes"
                            : "No"}
                        </td>

                        <td className="p-3">

                          <div className="flex flex-wrap gap-2">

                            {/* EDIT */}

                            <button
                              type="button"
                              onClick={() =>
                                editPolicy(
                                  policy
                                )
                              }
                              className="rounded bg-blue-600 px-3 py-2 text-white hover:bg-blue-700"
                            >
                              Edit
                            </button>

                            {/* PATCH */}

                            <button
                              type="button"
                              onClick={() =>
                                handlePatch(
                                  policy
                                )
                              }
                              className="rounded bg-purple-600 px-3 py-2 text-white hover:bg-purple-700"
                            >
                              PATCH
                            </button>

                            {/* DELETE */}

                            <button
                              type="button"
                              onClick={() =>
                                handleDelete(
                                  policy.id
                                )
                              }
                              className="rounded bg-red-600 px-3 py-2 text-white hover:bg-red-700"
                            >
                              Delete
                            </button>

                          </div>

                        </td>

                      </tr>

                    )
                  )}

                </tbody>

              </table>

            </div>

          )}

        </div>

      </div>

    </main>
  );
}