package com.caverock.bplist;

public class Result<T>
{
    private boolean  isSuccess;  // successful result?
    private String   message;    // failure error message
    private T        value;      // result value if successful


    // Disable empty constructor
    private Result()
    {
    }

    private Result(T value, String errorMessage)
    {
        this.value = value;
        this.message = errorMessage;
        this.isSuccess = (errorMessage == null);
    }


    public Result(T value)
    {
        this(value, null);
    }


    public Result(String errorMessage)
    {
        this(null, errorMessage);
    }


    /*
     * For the case when T is a String.
     * We need a way to create an error Result.
     */
    public Result(boolean isSuccess, String errorMessage)
    {
        if (isSuccess)
            throw new IllegalArgumentException("isSuccess must be false");
        this.isSuccess = isSuccess;  // always false
        this.value = null;
        this.message = errorMessage;
    }


    public boolean isSuccess()
    {
        return isSuccess;
    }

    public String getMessage()
    {
        return message;
    }

    public T getValue()
    {
        return value;
    }


}
